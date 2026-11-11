package com.tokoku.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BrevoEmailSenderTest {
    private static final String URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final BrevoEmailSender sender = new BrevoEmailSender(builder, URL, "rahasia-kunci", "TokoKu <toko@example.com>");

    @Test
    void sendsDocumentedPayload() {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "rahasia-kunci"))
                .andExpect(jsonPath("$.sender.email").value("toko@example.com"))
                .andExpect(jsonPath("$.sender.name").value("TokoKu"))
                .andExpect(jsonPath("$.to[0].email").value("budi@example.com"))
                .andExpect(jsonPath("$.subject").value("Halo"))
                .andExpect(jsonPath("$.htmlContent").value("<p>hi</p>"))
                .andExpect(jsonPath("$.textContent").value("hi"))
                .andExpect(jsonPath("$.attachment[0].name").value("INV.pdf"))
                .andExpect(jsonPath("$.attachment[0].content").value("QUJD")) // base64("ABC")
                .andRespond(withStatus(org.springframework.http.HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON).body("{\"messageId\":\"<1@x>\"}"));
        sender.send(new EmailMessage("budi@example.com", "Halo", "<p>hi</p>", "hi")
                .withAttachments(List.of(new EmailMessage.Attachment("INV.pdf", "application/pdf", "ABC".getBytes()))));
        server.verify();
    }

    @Test
    void errorDoesNotLeakKeyOrBody() {
        server.expect(requestTo(URL)).andRespond(withBadRequest().contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":\"invalid_parameter\",\"message\":\"sender not verified rahasia-kunci\"}"));
        assertThatThrownBy(() -> sender.send(new EmailMessage("a@b.c", "s", "h", "t")))
                .isInstanceOf(IllegalStateException.class).hasMessage("Brevo menolak email: HTTP 400")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("rahasia"));
    }

    @Test
    void invalidFromFailsFast() {
        assertThatThrownBy(() -> new BrevoEmailSender(RestClient.builder(), URL, "k", "bukan alamat"))
                .isInstanceOf(IllegalStateException.class);
    }
}
