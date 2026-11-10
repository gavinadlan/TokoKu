package com.tokoku.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.tokoku.common.PaymentGatewayException;

/**
 * Midtrans Snap (sandbox/produksi ditentukan oleh URL dan kunci). Format request, auth Basic, dan rumus signature
 * dicek ke dokumentasi resmi Midtrans. Batas panjang order_id dan nama item TIDAK tertulis di halaman yang dibaca,
 * jadi dipatuhi secara konservatif (order_id <= 50, nama item <= 50): belum terverifikasi.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "midtrans", matchIfMissing = true)
public class MidtransSnapGateway implements PaymentGateway {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MidtransSnapGateway.class);

    private final String serverKey;
    private final String clientKey;
    private final String snapUrl;
    private final String finishUrl;
    static final String FINISH_PATH = "/pembayaran/selesai"; // rute frontend; Midtrans menambahkan ?order_id=...&status_code=...&transaction_status=...
    private final RestClient http;

    public MidtransSnapGateway(
            @Value("${app.payment.midtrans.server-key:}") String serverKey,
            @Value("${app.payment.midtrans.client-key:}") String clientKey,
            @Value("${app.payment.midtrans.snap-url}") String snapUrl,
            @Value("${app.frontend.base-url:}") String frontendBaseUrl,
            org.springframework.core.env.Environment env) {
        String base = frontendBaseUrl.strip().replaceAll("/+$", "");
        boolean local = base.isEmpty() || base.matches("(?i)https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?");
        if (local && env.acceptsProfiles(org.springframework.core.env.Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "APP_FRONTEND_BASE_URL must be set to the public frontend URL (not empty or localhost) in the prod profile");
        }
        this.finishUrl = base + FINISH_PATH;
        if (serverKey.isBlank() || clientKey.isBlank()) {
            throw new IllegalStateException(
                    "app.payment.provider=midtrans requires MIDTRANS_SERVER_KEY and MIDTRANS_CLIENT_KEY "
                            + "(or set APP_PAYMENT_PROVIDER=fake for local development without Midtrans)");
        }
        this.serverKey = serverKey;
        log.info("Payment provider: midtrans ({}) via {}", serverKey.startsWith("SB-") || snapUrl.contains("sandbox") ? "sandbox" : "production", java.net.URI.create(snapUrl).getHost());
        this.clientKey = clientKey;
        this.snapUrl = snapUrl;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String clientKey() { return clientKey; }

    @Override
    public PaymentSession createPayment(PaymentRequest r) {
        Map<String, Object> body = buildBody(r);
        log.info("Snap request order_id={} callbacks.finish={}", r.providerRef(), finishUrl);
        try {
            Map<?, ?> response = http.post().uri(snapUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                            .encodeToString((serverKey + ":").getBytes(StandardCharsets.UTF_8)))
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Map.class);
            if (response == null || response.get("token") == null || response.get("redirect_url") == null) {
                throw new PaymentGatewayException("Midtrans response did not contain token and redirect_url");
            }
            return new PaymentSession(String.valueOf(response.get("token")), String.valueOf(response.get("redirect_url")));
        } catch (RestClientResponseException e) {
            // Sengaja tidak menyertakan header/kunci; hanya status dan badan error dari Midtrans
            throw new PaymentGatewayException("Midtrans rejected the request: HTTP " + e.getStatusCode().value()
                    + " " + abbreviate(e.getResponseBodyAsString()), e);
        } catch (PaymentGatewayException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new PaymentGatewayException("Could not reach Midtrans", e);
        }
    }

    Map<String, Object> buildBody(PaymentRequest r) {
        long gross = wholeRupiah(r.amount());
        List<Map<String, Object>> items = new ArrayList<>();
        long sum = 0;
        for (Item i : r.items()) {
            long price = wholeRupiah(i.price());
            sum += price * i.quantity();
            items.add(Map.of("id", i.id(), "price", price, "quantity", i.quantity(), "name", truncate(i.name(), 50)));
        }
        if (sum != gross) {
            throw new PaymentGatewayException("Order total does not equal the sum of its items");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaction_details", Map.of("order_id", r.providerRef(), "gross_amount", gross));
        body.put("item_details", items);
        body.put("customer_details", Map.of("first_name", truncate(r.customerName(), 50),
                "email", r.customerEmail(), "phone", r.customerPhone()));
        body.put("callbacks", Map.of("finish", finishUrl));
        body.put("expiry", Map.of("unit", "minutes", "duration", r.expiryMinutes()));
        return body;
    }

    @Override
    public boolean verifySignature(Notification n) {
        return MidtransSignature.matches(
                MidtransSignature.compute(n.orderId(), n.statusCode(), n.grossAmount(), serverKey), n.signatureKey());
    }

    // gross_amount Midtrans bertipe integer: hanya rupiah bulat yang diterima
    private static long wholeRupiah(BigDecimal amount) {
        try {
            return amount.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException e) {
            throw new PaymentGatewayException("Amount must be a whole number of rupiah: " + amount);
        }
    }

    private static String truncate(String s, int max) { return s.length() <= max ? s : s.substring(0, max); }

    private static String abbreviate(String s) { return s == null ? "" : s.length() <= 300 ? s : s.substring(0, 300); }
}
