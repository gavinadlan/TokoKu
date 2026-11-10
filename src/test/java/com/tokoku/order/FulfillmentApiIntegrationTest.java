package com.tokoku.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.payment.FakePaymentGateway;
import com.tokoku.product.Product;
import com.tokoku.store.StoreService;
import com.tokoku.support.ApiTestBase;

/** Gate B2: ongkir dihitung server, jalur kirim dan ambil punya state machine sendiri, kode ambil, nominal konsisten. */
class FulfillmentApiIntegrationTest extends ApiTestBase {

    @Autowired StoreService storeService;
    @Autowired FakePaymentGateway gateway;

    @AfterEach
    void resetStore() {
        configureStore("", "", "", "0", null, true, true);
    }

    private void configureStore(String name, String address, String phone, String fee, String threshold, boolean delivery, boolean pickup) {
        storeService.update(new com.tokoku.store.StoreDtos.StoreSettingsRequest(name.isEmpty() ? "TokoKu" : name,
                address.isEmpty() ? "x" : address, phone.isEmpty() ? "x" : phone, "", "", "", delivery, pickup,
                new BigDecimal(fee), threshold == null ? null : new BigDecimal(threshold), "", null, null));
        if (address.isEmpty()) blankIdentity();
    }

    // identitas kosong = "belum dikonfigurasi" (seperti produksi sebelum admin mengisi)
    private void blankIdentity() {
        jdbc().update("update store_settings set address = '', phone = ''");
    }

    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private org.springframework.jdbc.core.JdbcTemplate jdbc() { return jdbcTemplate; }

    private void configured(String fee, String threshold) {
        configureStore("TokoKu", "Jl. Contoh No. 1", "0800-0000", fee, threshold, true, true);
    }

    private String checkout(String token, String type, String address, int expectedStatus) throws Exception {
        String addr = address == null ? "" : ",\"shippingAddress\":\"" + address + "\"";
        return send(post("/api/orders"), token,
                "{\"fulfillmentType\":\"%s\",\"shippingName\":\"Budi\",\"shippingPhone\":\"08123456789\"%s}".formatted(type, addr))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
    }

    private long order(String token, Product p, int qty, String type) throws Exception {
        addToCart(token, p.getId(), qty);
        String body = checkout(token, type, type.equals("DELIVERY") ? "Jl. Kopi No. 12, Jakarta Pusat" : null, 201);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String pay(String token, long orderId) throws Exception {
        String body = send(post("/api/orders/" + orderId + "/pay"), token, null).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.providerRef");
    }

    private void settle(String token, long orderId) throws Exception {
        String ref = pay(token, orderId);
        send(post("/api/dev/payments/" + ref + "/simulate").param("status", "settlement"), null, null).andExpect(status().isOk());
    }

    private com.jayway.jsonpath.DocumentContext getOrder(String token, long id) throws Exception {
        return JsonPath.parse(send(get("/api/orders/" + id), token, null).andReturn().getResponse().getContentAsString());
    }

    @Test
    void deliveryFeeIsAddedAndFreeAboveThreshold() throws Exception {
        configured("15000", "100000");
        String token = userToken();
        Product cheap = product(10, "40000");
        addToCart(token, cheap.getId(), 1);
        String o1 = checkout(token, "DELIVERY", "Jl. Kopi No. 12, Jakarta Pusat", 201);
        assertThat(((Number) JsonPath.read(o1, "$.shippingFee")).doubleValue()).isEqualTo(15000.0);
        assertThat(((Number) JsonPath.read(o1, "$.totalAmount")).doubleValue()).isEqualTo(55000.0);
        assertThat(((Number) JsonPath.read(o1, "$.subtotal")).doubleValue()).isEqualTo(40000.0);

        addToCart(token, cheap.getId(), 3); // subtotal 120.000 >= ambang
        String o2 = checkout(token, "DELIVERY", "Jl. Kopi No. 12, Jakarta Pusat", 201);
        assertThat(((Number) JsonPath.read(o2, "$.shippingFee")).doubleValue()).isZero();
        assertThat(((Number) JsonPath.read(o2, "$.totalAmount")).doubleValue()).isEqualTo(120000.0);
    }

    @Test
    void clientCannotInjectShippingFee() throws Exception {
        configured("15000", null);
        String token = userToken();
        addToCart(token, product(5, "10000").getId(), 1);
        String body = send(post("/api/orders"), token,
                "{\"fulfillmentType\":\"DELIVERY\",\"shippingFee\":0,\"shippingName\":\"B\",\"shippingPhone\":\"0812345678\",\"shippingAddress\":\"Jl. Kopi No. 12, Jakarta\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(((Number) JsonPath.read(body, "$.shippingFee")).doubleValue()).isEqualTo(15000.0);
    }

    @Test
    void pickupNeedsNoAddressAndHasNoFee() throws Exception {
        configured("15000", null);
        String token = userToken();
        addToCart(token, product(5, "25000").getId(), 2);
        String body = checkout(token, "PICKUP", null, 201);
        assertThat((String) JsonPath.read(body, "$.fulfillmentType")).isEqualTo("PICKUP");
        assertThat(((Number) JsonPath.read(body, "$.shippingFee")).doubleValue()).isZero();
        assertThat(((Number) JsonPath.read(body, "$.totalAmount")).doubleValue()).isEqualTo(50000.0);
        assertThat((Object) JsonPath.read(body, "$.shippingAddress")).isNull();
        assertThat((Object) JsonPath.read(body, "$.pickupCode")).isNull(); // baru dibuat saat dibayar
    }

    @Test
    void deliveryWithoutAddressIsRejected() throws Exception {
        configured("0", null);
        String token = userToken();
        addToCart(token, product(5, "25000").getId(), 1);
        checkout(token, "DELIVERY", null, 400);
        checkout(token, "DELIVERY", "pendek", 400);
    }

    @Test
    void disabledOrUnconfiguredMethodsAreRefused() throws Exception {
        String token = userToken();
        addToCart(token, product(5, "25000").getId(), 1);
        // toko belum dikonfigurasi: ambil di toko tidak tersedia (alamat toko belum ada)
        checkout(token, "PICKUP", null, 409);
        configureStore("TokoKu", "Jl. Contoh No. 1", "0800-0000", "0", null, false, true);
        checkout(token, "DELIVERY", "Jl. Kopi No. 12, Jakarta Pusat", 409);
        configureStore("TokoKu", "Jl. Contoh No. 1", "0800-0000", "0", null, true, false);
        checkout(token, "PICKUP", null, 409);
    }

    @Test
    void pickupFlowGeneratesCodeAndRequiresMatchingCodeCaseInsensitively() throws Exception {
        configured("0", null);
        String token = userToken();
        String admin = adminToken();
        long id = order(token, product(5, "30000"), 1, "PICKUP");
        settle(token, id);

        String code = getOrder(token, id).read("$.pickupCode");
        assertThat(code).matches("[ABCDEFGHJKMNPQRSTUVWXYZ2-9]{6}");

        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"PROCESSING\"}").andExpect(status().isOk());
        // jalur ambil: SHIPPED ditolak, READY_FOR_PICKUP diterima
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"SHIPPED\",\"courier\":\"JNE\",\"trackingNumber\":\"1\"}").andExpect(status().isConflict());
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"READY_FOR_PICKUP\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupReadyAt").isNotEmpty());
        // selesai: tanpa kode / kode salah = 400 dan status tidak berubah
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"COMPLETED\"}").andExpect(status().isBadRequest());
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"COMPLETED\",\"pickupCode\":\"AAAAAA\"}").andExpect(status().isBadRequest());
        assertThat((String) getOrder(token, id).read("$.status")).isEqualTo("READY_FOR_PICKUP");
        // kode benar, huruf kecil dan berspasi
        String sloppy = (code.substring(0, 3) + " " + code.substring(3)).toLowerCase();
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"COMPLETED\",\"pickupCode\":\"%s\"}".formatted(sloppy))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deliveryFlowRequiresCourierAndTracking() throws Exception {
        configured("15000", null);
        String token = userToken();
        String admin = adminToken();
        long id = order(token, product(5, "30000"), 1, "DELIVERY");
        settle(token, id);
        assertThat((Object) getOrder(token, id).read("$.pickupCode")).isNull();

        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"PROCESSING\"}").andExpect(status().isOk());
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"READY_FOR_PICKUP\"}").andExpect(status().isConflict());
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"SHIPPED\"}").andExpect(status().isBadRequest());
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"SHIPPED\",\"courier\":\"JNE\"}").andExpect(status().isBadRequest());
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"SHIPPED\",\"courier\":\"JNE\",\"trackingNumber\":\"CGK1234567890\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.courier").value("JNE")).andExpect(jsonPath("$.trackingNumber").value("CGK1234567890"));
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"COMPLETED\"}").andExpect(status().isOk());
    }

    @Test
    void webhookAmountMustIncludeShippingFee() throws Exception {
        configured("15000", null);
        String token = userToken();
        long id = order(token, product(5, "40000"), 1, "DELIVERY"); // total 55.000
        String ref = pay(token, id);

        // nominal tanpa ongkir ditolak, nominal dengan ongkir diterima
        String bad = new tools.jackson.databind.json.JsonMapper().writeValueAsString(java.util.Map.of("order_id", ref, "status_code", "200",
                "gross_amount", "40000.00", "signature_key", gateway.signedNotification(ref, "200", "40000.00", "settlement").signatureKey(),
                "transaction_status", "settlement"));
        send(post("/api/payments/webhook"), null, bad).andExpect(status().isBadRequest());
        String good = new tools.jackson.databind.json.JsonMapper().writeValueAsString(java.util.Map.of("order_id", ref, "status_code", "200",
                "gross_amount", "55000.00", "signature_key", gateway.signedNotification(ref, "200", "55000.00", "settlement").signatureKey(),
                "transaction_status", "settlement"));
        send(post("/api/payments/webhook"), null, good).andExpect(status().isOk());
        assertThat((String) getOrder(token, id).read("$.status")).isEqualTo("PAID");
    }

    @Test
    void publicStoreEndpointExposesOnlySafeFields() throws Exception {
        configured("15000", "500000");
        mvc.perform(get("/api/store")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TokoKu"))
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.pickupEnabled").value(true))
                .andExpect(jsonPath("$.flatShippingFee").value(15000.0))
                .andExpect(jsonPath("$.taxId").doesNotExist())
                .andExpect(jsonPath("$.invoiceFooter").doesNotExist());
    }

    @Test
    void adminStoreSettingsAreAdminOnlyAndValidated() throws Exception {
        String admin = adminToken();
        send(get("/api/admin/store-settings"), userToken(), null).andExpect(status().isForbidden());
        send(get("/api/admin/store-settings"), null, null).andExpect(status().isUnauthorized());
        send(put("/api/admin/store-settings"), admin, "{\"name\":\"\",\"address\":\"\",\"phone\":\"\"}").andExpect(status().isBadRequest());
        send(put("/api/admin/store-settings"), admin,
                "{\"name\":\"TokoKu\",\"address\":\"Jl. Contoh No. 1\",\"phone\":\"0800\",\"deliveryEnabled\":true,\"pickupEnabled\":true,\"flatShippingFee\":12000,\"freeShippingThreshold\":300000,\"invoiceFooter\":\"Terima kasih\",\"taxId\":\"12.345\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.configured").value(true)).andExpect(jsonPath("$.taxId").value("12.345"));
    }

    @Test
    void adminSummaryCountsPaidOrdersAndRequiresAdmin() throws Exception {
        String user = userToken();
        send(get("/api/admin/orders/summary"), user, null).andExpect(status().isForbidden());
        send(get("/api/admin/orders/summary"), null, null).andExpect(status().isUnauthorized());

        String admin = adminToken();
        java.time.Instant before = java.time.Instant.now().minusSeconds(1);
        int paidBefore = ((Number) JsonPath.read(send(get("/api/admin/orders/summary"), admin, null).andReturn().getResponse().getContentAsString(), "$.needsAction")).intValue();
        long id = order(user, product(10, "40000"), 1, "DELIVERY");
        settle(user, id);

        var body = JsonPath.parse(send(get("/api/admin/orders/summary").param("since", before.toString()), admin, null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(body.read("$.needsAction", Integer.class)).isEqualTo(paidBefore + 1);
        assertThat(body.read("$.paidSince", Integer.class)).isGreaterThanOrEqualTo(1);
        var future = JsonPath.parse(send(get("/api/admin/orders/summary").param("since", java.time.Instant.now().plusSeconds(60).toString()), admin, null)
                .andReturn().getResponse().getContentAsString());
        assertThat(future.read("$.paidSince", Integer.class)).isZero();
    }
}
