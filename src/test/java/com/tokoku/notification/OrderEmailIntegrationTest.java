package com.tokoku.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.jayway.jsonpath.JsonPath;
import com.tokoku.payment.FakePaymentGateway;
import com.tokoku.support.ApiTestBase;

/** Email: setelah commit, idempoten, tidak menggagalkan transisi, penerima = pemilik pesanan. Pengirim dimock (tanpa SMTP). */
@TestPropertySource(properties = "app.mail.retry-backoff-ms=5")
class OrderEmailIntegrationTest extends ApiTestBase {

    @MockitoBean EmailSender sender;
    @Autowired OrderEmailService emails;
    @Autowired FakePaymentGateway gateway;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void delivering() { reset(sender); when(sender.delivers()).thenReturn(true); }

    private record Buyer(String email, String token) {}

    private Buyer buyer() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\",\"name\":\"Tester\"}".formatted(email, PASSWORD))).andExpect(status().isCreated());
        return new Buyer(email, login(email, PASSWORD));
    }

    private long order(Buyer b) throws Exception {
        addToCart(b.token(), product(10, "40000").getId(), 1);
        return checkout(b.token());
    }

    private void settle(Buyer b, long orderId) throws Exception {
        String body = send(post("/api/orders/" + orderId + "/pay"), b.token(), null).andReturn().getResponse().getContentAsString();
        String ref = JsonPath.read(body, "$.providerRef");
        send(post("/api/dev/payments/" + ref + "/simulate").param("status", "settlement"), null, null).andExpect(status().isOk());
    }

    private List<EmailMessage> sent(int atLeast) {
        var captor = org.mockito.ArgumentCaptor.forClass(EmailMessage.class);
        verify(sender, timeout(5000).atLeast(atLeast)).send(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void checkoutSendsOrderCreatedToTheOwnerOnly() throws Exception {
        Buyer b = buyer();
        long id = order(b);
        EmailMessage m = sent(1).get(0);
        assertThat(m.to()).isEqualTo(b.email());
        assertThat(m.subject()).contains(String.format("%04d", id)).contains("dibuat");
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(logStatus(id, "ORDER_CREATED")).isEqualTo("SENT"));
    }

    @Test
    void repeatedWebhookDoesNotSendPaidEmailTwice() throws Exception {
        Buyer b = buyer();
        long id = order(b);
        settle(b, id);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(logStatus(id, "PAID")).isEqualTo("SENT"));
        // webhook ulang (payment sudah tidak pending) dan pemanggilan langsung dengan jenis sama
        String ref = jdbc.queryForObject("select provider_ref from payments where order_id = ?", String.class, id);
        send(post("/api/dev/payments/" + ref + "/simulate").param("status", "settlement"), null, null);
        assertThat(emails.handle(id, EmailKind.PAID)).isFalse();
        long paidMails = sent(2).stream().filter(m -> m.subject().contains("Pembayaran")).count();
        assertThat(paidMails).isEqualTo(1);
    }

    @Test
    void senderFailureNeverBreaksTheStatusTransition() throws Exception {
        doThrow(new IllegalStateException("smtp down")).when(sender).send(any());
        Buyer b = buyer();
        long id = order(b); // checkout tetap 201
        settle(b, id);      // webhook tetap 200 (di dalam settle)
        String admin = adminToken();
        send(patch("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"PROCESSING\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select status from orders where id = ?", String.class, id)).isEqualTo("PROCESSING");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(logStatus(id, "PROCESSING")).isEqualTo("FAILED"));
        verify(sender, atLeast(3)).send(any()); // 3 percobaan untuk satu jenis, lalu menyerah
    }

    @Test
    void noopSenderSkipsWithoutLogRow() throws Exception {
        when(sender.delivers()).thenReturn(false);
        Buyer b = buyer();
        long id = order(b);
        assertThat(emails.handle(id, EmailKind.ORDER_CREATED)).isFalse();
        verify(sender, after(300).never()).send(any());
        assertThat(logStatus(id, "ORDER_CREATED")).isNull();
    }

    @Test
    void cancelAndPickupAdminFlowEmailsEachKindOnce() throws Exception {
        Buyer b = buyer();
        long id = order(b);
        send(post("/api/orders/" + id + "/cancel"), b.token(), null).andExpect(status().isOk());
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(logStatus(id, "CANCELLED")).isEqualTo("SENT"));
    }

    private String logStatus(long orderId, String kind) {
        return jdbc.query("select status from email_log where order_id = ? and kind = ?", rs -> rs.next() ? rs.getString(1) : null, orderId, kind);
    }
}
