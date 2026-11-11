package com.tokoku.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.support.ApiTestBase;

/** Rekonsiliasi lewat Status API (gateway palsu): jalur sama dengan webhook, aman, idempoten, dibatasi per pengguna. */
@TestPropertySource(properties = "app.payment.refresh-per-minute=6")
class PaymentReconcileIntegrationTest extends ApiTestBase {

    @Autowired FakePaymentGateway gateway;
    @Autowired PaymentReconciler reconciler;
    @Autowired JdbcTemplate jdbc;

    private record Paying(String token, long orderId, String ref, String amount) {}

    private Paying pending() throws Exception {
        String token = userToken();
        addToCart(token, product(10, "40000").getId(), 2);
        long id = checkout(token);
        String body = send(post("/api/orders/" + id + "/pay"), token, null).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Paying(token, id, JsonPath.read(body, "$.providerRef"), String.valueOf(JsonPath.<Object>read(body, "$.amount")));
    }

    private String refresh(Paying p) throws Exception {
        return send(post("/api/orders/" + p.orderId() + "/payment/refresh"), p.token(), null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String orderStatus(long id) { return jdbc.queryForObject("select status from orders where id = ?", String.class, id); }

    @ParameterizedTest(name = "{0}/{1} -> {2}")
    @CsvSource({
            "settlement, , PAID, PAID",
            "capture, accept, PAID, PAID",
            "capture, challenge, PENDING_PAYMENT, PENDING",
            "pending, , PENDING_PAYMENT, PENDING",
            "authorize, , PENDING_PAYMENT, PENDING",
            "expire, , CANCELLED, EXPIRED",
            "cancel, , CANCELLED, FAILED",
            "deny, , CANCELLED, FAILED",
            "failure, , CANCELLED, FAILED" })
    void eachMidtransStatusMapsLikeTheWebhook(String tx, String fraud, String orderStatus, String paymentStatus) throws Exception {
        Paying p = pending();
        gateway.setProviderStatus(p.ref(), p.amount(), tx, fraud);
        assertThat(JsonPath.<String>read(refresh(p), "$.status")).isEqualTo(orderStatus);
        assertThat(jdbc.queryForObject("select status from payments where provider_ref = ?", String.class, p.ref())).isEqualTo(paymentStatus);
        if (orderStatus.equals("PAID")) assertThat(JsonPath.<String>read(refresh(p), "$.invoiceNumber")).startsWith("INV-");
        if (orderStatus.equals("CANCELLED")) assertThat(products.findAll().stream().allMatch(x -> x.getStock() >= 0)).isTrue();
    }

    @Test
    void unknownAtProviderLeavesOrderPending() throws Exception {
        Paying p = pending(); // tidak ada status di provider (= Midtrans 404: belum memilih metode bayar)
        assertThat(JsonPath.<String>read(refresh(p), "$.status")).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void amountMismatchIsRejectedAndChangesNothing() throws Exception {
        Paying p = pending();
        gateway.setProviderStatus(p.ref(), "1.00", "settlement", null);
        send(post("/api/orders/" + p.orderId() + "/payment/refresh"), p.token(), null).andExpect(status().isBadRequest());
        assertThat(orderStatus(p.orderId())).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void anotherUsersOrderIsNotFoundAndUntouched() throws Exception {
        Paying p = pending();
        gateway.setProviderStatus(p.ref(), p.amount(), "settlement", null);
        send(post("/api/orders/" + p.orderId() + "/payment/refresh"), userToken(), null).andExpect(status().isNotFound());
        send(post("/api/orders/" + p.orderId() + "/payment/refresh"), null, null).andExpect(status().isUnauthorized());
        assertThat(orderStatus(p.orderId())).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void webhookAndRefreshTogetherAreIdempotent() throws Exception {
        Paying p = pending();
        gateway.setProviderStatus(p.ref(), p.amount(), "settlement", null);
        refresh(p);
        String invoice = jdbc.queryForObject("select invoice_number from orders where id = ?", String.class, p.orderId());
        // webhook datang belakangan, lalu refresh lagi
        send(post("/api/dev/payments/" + p.ref() + "/simulate").param("status", "settlement"), null, null).andExpect(status().isOk());
        refresh(p);
        assertThat(orderStatus(p.orderId())).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("select invoice_number from orders where id = ?", String.class, p.orderId())).isEqualTo(invoice);
        assertThat(jdbc.queryForObject("select count(*) from payments where order_id = ? and status = 'PAID'", Integer.class, p.orderId())).isEqualTo(1);
    }

    @Test
    void refreshIsRateLimitedPerUser() throws Exception {
        Paying p = pending();
        for (int i = 0; i < 6; i++) refresh(p);
        send(post("/api/orders/" + p.orderId() + "/payment/refresh"), p.token(), null)
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.status").value(429));
        Paying other = pending(); // pengguna lain tidak terkena
        refresh(other);
    }

    @Test
    void schedulerReconcilesRecentPendingPaymentsOnly() throws Exception {
        Paying recent = pending();
        Paying old = pending();
        Paying stale = pending();
        jdbc.update("update payments set created_at = now() - interval '10 minutes' where provider_ref = ?", old.ref());
        jdbc.update("update payments set created_at = now() - interval '25 hours' where provider_ref = ?", stale.ref());
        for (Paying p : new Paying[] { recent, old, stale }) gateway.setProviderStatus(p.ref(), p.amount(), "settlement", null);

        reconciler.reconcileRecent();

        assertThat(orderStatus(old.orderId())).isEqualTo("PAID");
        assertThat(orderStatus(recent.orderId())).isEqualTo("PENDING_PAYMENT"); // < 2 menit: beri webhook kesempatan dulu
        assertThat(orderStatus(stale.orderId())).isNotEqualTo("PAID");          // > 24 jam: di luar jaring pengaman
    }
}
