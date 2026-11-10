package com.tokoku.notification;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Hanya profil dev dan hanya admin (di bawah /api/admin): mencoba kredensial SMTP dengan satu email uji ke alamat pilihan Anda. */
@Hidden
@RestController
@Profile("dev")
@RequestMapping("/api/admin/dev/test-email")
public class DevEmailController {

    record TestEmailRequest(@NotBlank @Email String to) {}

    private final EmailSender sender;
    private final String baseUrl;

    public DevEmailController(EmailSender sender, @Value("${app.frontend.base-url:http://localhost:5173}") String baseUrl) {
        this.sender = sender;
        this.baseUrl = baseUrl;
    }

    @PostMapping
    public Map<String, Object> send(@Valid @RequestBody TestEmailRequest r) {
        if (!sender.delivers()) {
            return Map.of("sent", false, "reason", "Pengirim no-op: isi MAIL_USERNAME dan MAIL_PASSWORD di .env (dan APP_MAIL_ENABLED bukan false), lalu jalankan ulang backend.");
        }
        EmailMessage m = MailTemplates.render(EmailKind.READY_FOR_PICKUP, OrderEmailService.sample(r.to(), baseUrl));
        sender.send(new EmailMessage(r.to(), "[Uji] " + m.subject(), m.html(), m.text()));
        return Map.of("sent", true, "to", EmailSender.mask(r.to()));
    }
}
