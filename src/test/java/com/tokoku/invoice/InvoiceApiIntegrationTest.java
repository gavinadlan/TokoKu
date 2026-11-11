package com.tokoku.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.payment.FakePaymentGateway;
import com.tokoku.product.Product;
import com.tokoku.store.StoreDtos.StoreSettingsRequest;
import com.tokoku.store.StoreService;
import com.tokoku.support.ApiTestBase;

/** Gate B4: PDF valid, nomor unik dan stabil, 409 sebelum bayar atau saat toko belum dikonfigurasi, 404 milik orang lain. */
class InvoiceApiIntegrationTest extends ApiTestBase {

    @Autowired StoreService storeService;
    @Autowired FakePaymentGateway gateway;
    @Autowired InvoiceNumberService numbers;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void resetStore() {
        storeService.update(new StoreSettingsRequest("TokoKu", "x", "x", "", "", "", true, true, BigDecimal.ZERO, null, "", null, null));
        jdbc.update("update store_settings set address = '', phone = ''");
    }

    private void configureStore() {
        storeService.update(new StoreSettingsRequest("TokoKu", "Jl. Contoh No. 1, Kota Anda", "0800-0000-0000", "halo@tokoku.example",
                "Senin sampai Sabtu, 09.00 sampai 17.00", "Tunjukkan kode ambil di kasir.", true, true, new BigDecimal("15000"), null,
                "Terima kasih telah berbelanja di TokoKu.", null, null));
    }

    private long order(String token, String type, int qty) throws Exception {
        Product p = product(10, "40000");
        addToCart(token, p.getId(), qty);
        String addr = type.equals("DELIVERY") ? ",\"shippingAddress\":\"Jl. Kopi No. 12, Jakarta Pusat\"" : "";
        String body = send(post("/api/orders"), token,
                "{\"fulfillmentType\":\"%s\",\"shippingName\":\"Siti Rahmawati\",\"shippingPhone\":\"081234567890\"%s}".formatted(type, addr))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String ref(String token, long id) throws Exception {
        return JsonPath.read(send(post("/api/orders/" + id + "/pay"), token, null).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.providerRef");
    }

    private void settle(String token, long id) throws Exception {
        send(post("/api/dev/payments/" + ref(token, id) + "/simulate").param("status", "settlement"), null, null).andExpect(status().isOk());
    }

    private String invoiceNumber(String token, long id) throws Exception {
        return JsonPath.read(send(get("/api/orders/" + id), token, null).andReturn().getResponse().getContentAsString(), "$.invoiceNumber");
    }

    private String pdfText(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) { return new PDFTextStripper().getText(doc); }
    }

    @Test
    void paidDeliveryOrderYieldsPdfWithNumberTotalAndShipping() throws Exception {
        configureStore();
        String token = userToken();
        long id = order(token, "DELIVERY", 2); // 80.000 + ongkir 15.000
        settle(token, id);
        String number = invoiceNumber(token, id);
        assertThat(number).matches("INV-\\d{6}-\\d{5}");

        byte[] pdf = send(get("/api/orders/" + id + "/invoice"), token, null).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(number + ".pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        String t = pdfText(pdf);
        assertThat(t).contains(number, "Rp 95.000", "Rp 15.000", "Biaya kirim", "Tester", "Siti Rahmawati", "Jl. Kopi No. 12", "Lunas", "Simulasi pembayaran",
                "Terima kasih telah berbelanja di TokoKu.", "Jl. Contoh No. 1, Kota Anda");
    }

    @Test
    void pickupInvoiceCarriesStoreLocationAndPickupCode() throws Exception {
        configureStore();
        String token = userToken();
        long id = order(token, "PICKUP", 1);
        settle(token, id);
        String code = JsonPath.read(send(get("/api/orders/" + id), token, null).andReturn().getResponse().getContentAsString(), "$.pickupCode");

        String t = pdfText(send(get("/api/orders/" + id + "/invoice"), token, null).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        assertThat(t).contains("Pengambilan di toko", "Senin sampai Sabtu", "Rp 40.000").doesNotContain("Biaya kirim");
        assertThat(t.replaceAll("\\s", "")).contains(code); // letter-spacing bisa memecah huruf saat ekstraksi teks
    }

    @Test
    void adminCanDownloadAnyPaidInvoiceAndStoreTokenCannotUseAdminRoute() throws Exception {
        configureStore();
        String token = userToken();
        long id = order(token, "DELIVERY", 1);
        settle(token, id);
        send(get("/api/admin/orders/" + id + "/invoice"), adminToken(), null).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/pdf"));
        send(get("/api/admin/orders/" + id + "/invoice"), token, null).andExpect(status().isForbidden());
        send(get("/api/orders/" + id + "/invoice"), adminToken(), null).andExpect(status().isForbidden()); // token admin tidak untuk rute pembeli
        send(get("/api/orders/" + id + "/invoice"), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void unpaidOrderIs409AndOtherUsersOrderIs404() throws Exception {
        configureStore();
        String token = userToken();
        long id = order(token, "DELIVERY", 1);
        send(get("/api/orders/" + id + "/invoice"), token, null).andExpect(status().isConflict());
        settle(token, id);
        send(get("/api/orders/" + id + "/invoice"), userToken(), null).andExpect(status().isNotFound());
        send(get("/api/orders/999999/invoice"), token, null).andExpect(status().isNotFound());
    }

    @Test
    void unconfiguredStoreGives409WithAClearMessageAndWorksAfterConfiguring() throws Exception {
        // default tes = seperti produksi: identitas toko kosong
        String token = userToken();
        long id = order(token, "DELIVERY", 1);
        settle(token, id);
        send(get("/api/orders/" + id + "/invoice"), token, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Store settings are incomplete")));
        send(get("/api/admin/orders/" + id + "/invoice"), adminToken(), null).andExpect(status().isConflict());

        configureStore();
        send(get("/api/orders/" + id + "/invoice"), token, null).andExpect(status().isOk());
    }

    @Test
    void duplicateWebhookDoesNotChangeTheInvoiceNumber() throws Exception {
        configureStore();
        String token = userToken();
        long id = order(token, "DELIVERY", 1);
        String ref = ref(token, id);
        String gross = "55000.00"; // 40.000 + ongkir 15.000
        var settle = gateway.signedNotification(ref, "200", gross, "settlement");
        String json = new tools.jackson.databind.json.JsonMapper().writeValueAsString(Map.of("order_id", ref, "status_code", "200",
                "gross_amount", gross, "signature_key", settle.signatureKey(), "transaction_status", "settlement", "payment_type", "qris"));

        send(post("/api/payments/webhook"), null, json).andExpect(status().isOk());
        String first = invoiceNumber(token, id);
        send(post("/api/payments/webhook"), null, json).andExpect(status().isOk());
        send(post("/api/payments/webhook"), null, json).andExpect(status().isOk());
        assertThat(invoiceNumber(token, id)).isEqualTo(first);
        // metode pembayaran dari notifikasi tampil di invoice
        assertThat(pdfText(send(get("/api/orders/" + id + "/invoice"), token, null).andReturn().getResponse().getContentAsByteArray())).contains("QRIS");
    }

    @Test
    void invoiceNumbersAreUniqueAndGaplessUnderConcurrency() throws Exception {
        int n = 20;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CyclicBarrier barrier = new CyclicBarrier(n);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                barrier.await();
                return tx.execute(s -> numbers.next());
            }));
        }
        Set<String> all = new HashSet<>();
        List<Integer> seq = new ArrayList<>();
        for (Future<String> f : futures) {
            String num = f.get(30, TimeUnit.SECONDS);
            all.add(num);
            seq.add(Integer.parseInt(num.substring(num.lastIndexOf('-') + 1)));
        }
        pool.shutdown();
        assertThat(all).hasSize(n);
        java.util.Collections.sort(seq);
        assertThat(seq.get(n - 1) - seq.get(0)).isEqualTo(n - 1); // berurutan tanpa celah
    }
}
