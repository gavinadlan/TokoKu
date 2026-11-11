package com.tokoku.notification;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

/**
 * Brevo Transactional Email API: POST https://api.brevo.com/v3/smtp/email, header "api-key", 201 = diterima.
 * Dipakai di host gratis yang memblokir SMTP keluar. Pengirim (MAIL_FROM) harus sudah terverifikasi di Brevo.
 * Pesan error tidak memuat kunci: hanya status HTTP.
 */
public class BrevoEmailSender implements EmailSender {

    private final RestClient http;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public BrevoEmailSender(RestClient.Builder builder, String url, String apiKey, String from) {
        this.http = builder.baseUrl(url).build();
        this.apiKey = apiKey;
        try {
            InternetAddress a = new InternetAddress(from, true);
            this.senderEmail = a.getAddress();
            this.senderName = a.getPersonal();
        } catch (AddressException e) {
            throw new IllegalStateException("MAIL_FROM tidak valid (format: Nama <alamat@domain>)");
        }
    }

    @Override
    public void send(EmailMessage m) {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, String> sender = new LinkedHashMap<>();
        sender.put("email", senderEmail);
        if (senderName != null) sender.put("name", senderName);
        body.put("sender", sender);
        body.put("to", List.of(Map.of("email", m.to())));
        body.put("subject", m.subject());
        body.put("htmlContent", m.html());
        if (m.text() != null) body.put("textContent", m.text());
        if (!m.attachments().isEmpty()) {
            List<Map<String, String>> files = new ArrayList<>();
            for (EmailMessage.Attachment a : m.attachments()) {
                files.add(Map.of("name", a.filename(), "content", Base64.getEncoder().encodeToString(a.data())));
            }
            body.put("attachment", files);
        }
        try {
            http.post().header("api-key", apiKey).header("accept", "application/json")
                    .body(body).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Brevo menolak email: HTTP " + e.getStatusCode().value());
        }
        // Kegagalan jaringan (ResourceAccessException) dibiarkan naik: pemanggil mengulang
    }
}
