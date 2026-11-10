package com.tokoku.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.order.OrderRepository;
import com.tokoku.order.OrderStatus;
import com.tokoku.payment.PaymentGateway.Notification;
import com.tokoku.product.Product;
import com.tokoku.support.ApiTestBase;

class PaymentApiIntegrationTest extends ApiTestBase {

    @Autowired FakePaymentGateway gateway;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;

    record Fixture(String token, Product product, long orderId) {}

    private Fixture pendingOrder(int stock, int qty) throws Exception {
        String token = userToken();
        Product p = product(stock, "30000");
        addToCart(token, p.getId(), qty);
        return new Fixture(token, p, checkout(token));
    }

    private String payRef(Fixture f) throws Exception {
        String body = send(post("/api/orders/" + f.orderId() + "/pay"), f.token(), null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.providerRef");
    }

    private String gross(Fixture f, int qty) { return "%d.00".formatted(30000 * qty); }

    private Map<String, Object> body(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order_id", n.orderId());
        m.put("status_code", n.statusCode());
        m.put("gross_amount", n.grossAmount());
        m.put("signature_key", n.signatureKey());
        m.put("transaction_status", n.transactionStatus());
        m.put("fraud_status", n.fraudStatus());
        m.put("transaction_id", n.transactionId());
        m.put("payment_type", "bank_transfer"); // field tambahan dari Midtrans harus diabaikan
        return m;
    }

    private org.springframework.test.web.servlet.ResultActions webhook(Notification n) throws Exception {
        String json = new tools.jackson.databind.json.JsonMapper().writeValueAsString(body(n));
        return send(post("/api/payments/webhook"), null, json);
    }

    private String orderStatus(Fixture f) throws Exception {
        String b = send(get("/api/orders/" + f.orderId()), f.token(), null).andReturn().getResponse().getContentAsString();
        return JsonPath.read(b, "$.status");
    }

    private int stock(Fixture f) { return products.findById(f.product().getId()).orElseThrow().getStock(); }

    @Test
    void payCreatesPaymentAndIsIdempotent() throws Exception {
        Fixture f = pendingOrder(5, 2);
        String first = send(post("/api/orders/" + f.orderId() + "/pay"), f.token(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.redirectUrl").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(60000.0))
                .andReturn().getResponse().getContentAsString();
        String second = send(post("/api/orders/" + f.orderId() + "/pay"), f.token(), null).andReturn().getResponse().getContentAsString();

        assertThat((Integer) JsonPath.read(second, "$.paymentId")).isEqualTo((Integer) JsonPath.read(first, "$.paymentId"));
        assertThat(payments.findByOrderId(f.orderId())).hasSize(1);
    }

    @Test
    void onlyOwnerCanPayAndOnlyPendingOrders() throws Exception {
        Fixture f = pendingOrder(5, 1);
        send(post("/api/orders/" + f.orderId() + "/pay"), userToken(), null).andExpect(status().isNotFound());
        send(post("/api/orders/" + f.orderId() + "/pay"), null, null).andExpect(status().isUnauthorized());

        String ref = payRef(f);
        webhook(gateway.signedNotification(ref, "200", gross(f, 1), "settlement")).andExpect(status().isOk());
        send(post("/api/orders/" + f.orderId() + "/pay"), f.token(), null).andExpect(status().isConflict());
    }

    @Test
    void validSignatureMakesOrderPaid() throws Exception {
        Fixture f = pendingOrder(5, 2);
        String ref = payRef(f);

        webhook(gateway.signedNotification(ref, "200", gross(f, 2), "settlement")).andExpect(status().isOk());

        assertThat(orderStatus(f)).isEqualTo("PAID");
        assertThat(stock(f)).isEqualTo(3); // stok tetap berkurang
        assertThat(payments.findByProviderRef(ref).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payments.findByProviderRef(ref).orElseThrow().getPaidAt()).isNotNull();
    }

    @Test
    void wrongSignatureIsRejectedAndNothingChanges() throws Exception {
        Fixture f = pendingOrder(5, 1);
        String ref = payRef(f);
        Notification good = gateway.signedNotification(ref, "200", gross(f, 1), "settlement");
        Notification forged = new Notification(good.orderId(), good.statusCode(), good.grossAmount(),
                "0".repeat(128), good.transactionStatus(), null, "tx", null);

        webhook(forged).andExpect(status().isForbidden());
        webhook(new Notification(ref, "200", gross(f, 1), null, "settlement", null, "tx", null)).andExpect(status().isForbidden());

        assertThat(orderStatus(f)).isEqualTo("PENDING_PAYMENT");
        assertThat(payments.findByProviderRef(ref).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void duplicateWebhookIsIdempotent() throws Exception {
        Fixture f = pendingOrder(5, 1);
        String ref = payRef(f);
        Notification settle = gateway.signedNotification(ref, "200", gross(f, 1), "settlement");
        webhook(settle).andExpect(status().isOk());
        var paidAt = payments.findByProviderRef(ref).orElseThrow().getPaidAt();

        webhook(settle).andExpect(status().isOk());
        // notifikasi expire yang datang terlambat tidak boleh membatalkan order yang sudah dibayar
        webhook(gateway.signedNotification(ref, "200", gross(f, 1), "expire")).andExpect(status().isOk());

        assertThat(orderStatus(f)).isEqualTo("PAID");
        assertThat(stock(f)).isEqualTo(4);
        assertThat(payments.findByProviderRef(ref).orElseThrow().getPaidAt()).isEqualTo(paidAt);
    }

    @Test
    void amountMismatchIsRejected() throws Exception {
        Fixture f = pendingOrder(5, 2);
        String ref = payRef(f);

        webhook(gateway.signedNotification(ref, "200", "100.00", "settlement")).andExpect(status().isBadRequest());

        assertThat(orderStatus(f)).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void unknownReferenceIs404() throws Exception {
        webhook(gateway.signedNotification("TKU-0-nope", "200", "1.00", "settlement")).andExpect(status().isNotFound());
    }

    @Test
    void expireCancelsOrderAndRestoresStock() throws Exception {
        Fixture f = pendingOrder(5, 3);
        assertThat(stock(f)).isEqualTo(2);
        String ref = payRef(f);

        webhook(gateway.signedNotification(ref, "200", gross(f, 3), "expire")).andExpect(status().isOk());

        assertThat(orderStatus(f)).isEqualTo("CANCELLED");
        assertThat(stock(f)).isEqualTo(5);
        assertThat(payments.findByProviderRef(ref).orElseThrow().getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        // mengulang webhook tidak mengembalikan stok dua kali
        webhook(gateway.signedNotification(ref, "200", gross(f, 3), "expire")).andExpect(status().isOk());
        assertThat(stock(f)).isEqualTo(5);
    }

    @Test
    void denyCancelsAndPendingChangesNothing() throws Exception {
        Fixture f = pendingOrder(5, 1);
        String ref = payRef(f);
        webhook(gateway.signedNotification(ref, "201", gross(f, 1), "pending")).andExpect(status().isOk());
        assertThat(orderStatus(f)).isEqualTo("PENDING_PAYMENT");

        webhook(gateway.signedNotification(ref, "202", gross(f, 1), "deny")).andExpect(status().isOk());
        assertThat(orderStatus(f)).isEqualTo("CANCELLED");
        assertThat(stock(f)).isEqualTo(5);
    }

    @Test
    void devSimulateEndpointDrivesTheFlowAndAdminCanFulfil() throws Exception {
        Fixture f = pendingOrder(5, 1);
        String ref = payRef(f);

        send(post("/api/dev/payments/" + ref + "/simulate").param("status", "settlement"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("PAID"));
        send(post("/api/dev/payments/" + ref + "/simulate").param("status", "bogus"), null, null).andExpect(status().isBadRequest());

        String admin = adminToken();
        send(patch("/api/admin/orders/" + f.orderId() + "/status"), admin, "{\"status\":\"PROCESSING\"}").andExpect(status().isOk());
        send(patch("/api/admin/orders/" + f.orderId() + "/status"), admin, "{\"status\":\"SHIPPED\",\"courier\":\"JNE\",\"trackingNumber\":\"CGK123\"}").andExpect(status().isOk());
        send(patch("/api/admin/orders/" + f.orderId() + "/status"), admin, "{\"status\":\"COMPLETED\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(orders.findById(f.orderId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
}
