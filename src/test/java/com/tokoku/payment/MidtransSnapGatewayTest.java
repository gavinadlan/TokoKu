package com.tokoku.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import com.tokoku.common.PaymentGatewayException;
import com.tokoku.payment.PaymentGateway.Item;
import com.tokoku.payment.PaymentGateway.Notification;
import com.tokoku.payment.PaymentGateway.PaymentRequest;

// Tanpa jaringan sungguhan: server HTTP lokal (JDK) menangkap request yang dikirim gateway
class MidtransSnapGatewayTest {

    private static final String SERVER_KEY = "SB-Mid-server-EXAMPLE"; // nilai uji, bukan kunci asli

    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private volatile int status = 201;
    private volatile String responseJson = "{\"token\":\"tok-123\",\"redirect_url\":\"https://app.sandbox.midtrans.com/snap/v3/redirection/tok-123\"}";

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            path.set(ex.getRequestURI().getPath());
            auth.set(ex.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = responseJson.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void stop() { server.stop(0); }

    private MidtransSnapGateway gateway() {
        return new MidtransSnapGateway(SERVER_KEY, "SB-Mid-client-EXAMPLE",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/snap/v1/transactions");
    }

    private PaymentRequest request(String amount, String itemPrice) {
        return new PaymentRequest("TKU-7-abcd1234", new BigDecimal(amount),
                List.of(new Item("P1", "Kopi Arabika 250g", new BigDecimal(itemPrice), 2)),
                "Budi", "budi@example.com", "08123456789", 60);
    }

    @Test
    void sendsDocumentedRequestShapeAndParsesResponse() {
        var session = gateway().createPayment(request("90000.00", "45000.00"));

        assertThat(session.token()).isEqualTo("tok-123");
        assertThat(session.redirectUrl()).endsWith("/tok-123");
        assertThat(path.get()).isEqualTo("/snap/v1/transactions");
        assertThat(auth.get()).isEqualTo("Basic " + Base64.getEncoder().encodeToString((SERVER_KEY + ":").getBytes(StandardCharsets.UTF_8)));
        assertThat(body.get())
                .contains("\"order_id\":\"TKU-7-abcd1234\"")
                .contains("\"gross_amount\":90000")
                .contains("\"item_details\"").contains("\"price\":45000").contains("\"quantity\":2")
                .contains("\"customer_details\"").contains("\"email\":\"budi@example.com\"")
                .contains("\"expiry\"").contains("\"unit\":\"minutes\"").contains("\"duration\":60");
    }

    @Test
    void shippingFeeIsItsOwnLineAndGrossAmountIncludesIt() {
        PaymentRequest r = new PaymentRequest("TKU-8-abcd1234", new BigDecimal("105000.00"),
                List.of(new Item("P1", "Kopi Arabika Gayo 250g", new BigDecimal("45000.00"), 2),
                        new Item("SHIPPING", "Biaya kirim", new BigDecimal("15000.00"), 1)),
                "Budi", "budi@example.com", "08123456789", 60);
        gateway().createPayment(r);

        assertThat(body.get()).contains("\"gross_amount\":105000").contains("\"id\":\"SHIPPING\"")
                .contains("\"name\":\"Biaya kirim\"").contains("\"price\":15000");
    }

    @Test
    void rejectsFractionalRupiahAndMismatchedTotals() {
        assertThatThrownBy(() -> gateway().createPayment(request("90000.50", "45000.25")))
                .isInstanceOf(PaymentGatewayException.class);
        assertThatThrownBy(() -> gateway().createPayment(request("80000.00", "45000.00")))
                .isInstanceOf(PaymentGatewayException.class).hasMessageContaining("sum");
    }

    @Test
    void midtransErrorBecomesGatewayExceptionWithoutLeakingKey() {
        status = 401;
        responseJson = "{\"error_messages\":[\"Access denied due to unauthorized transaction\"]}";
        assertThatThrownBy(() -> gateway().createPayment(request("90000.00", "45000.00")))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("401")
                .hasMessageNotContaining(SERVER_KEY);
    }

    @Test
    void blankKeysFailFastWithClearMessage() {
        assertThatThrownBy(() -> new MidtransSnapGateway("", "", "http://x"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("MIDTRANS_SERVER_KEY");
    }

    @Test
    void signatureMatchesSha512FormulaFromMidtransDocs() {
        // SHA512(order_id + status_code + gross_amount + ServerKey), nilai dihitung independen dengan hashlib Python
        String expected = "a4d19dd92c22862e9a07e0046b35972422ef1e9511aa9b5f8ab5be68028221b9e4bf1869e2e9360e982d0746b53a245e308ef22360271911df1e868349d2e1b6";
        assertThat(MidtransSignature.compute("ORDER-101", "200", "1000.00", SERVER_KEY)).isEqualTo(expected);

        var gw = gateway();
        assertThat(gw.verifySignature(new Notification("ORDER-101", "200", "1000.00", expected, "settlement", null, "t", null))).isTrue();
        assertThat(gw.verifySignature(new Notification("ORDER-101", "200", "1000.01", expected, "settlement", null, "t", null))).isFalse();
        assertThat(gw.verifySignature(new Notification("ORDER-101", "200", "1000.00", null, "settlement", null, "t", null))).isFalse();
    }
}
