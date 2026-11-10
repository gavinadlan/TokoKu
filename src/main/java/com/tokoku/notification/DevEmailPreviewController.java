package com.tokoku.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Pratinjau template di browser, hanya profil dev. Memakai data contoh tetap (tanpa DB, tanpa data pembeli nyata),
 * jadi aman tanpa login: http://localhost:8080/api/dev/email-preview/PAID?variant=pickup|delivery[&format=text]
 */
@Hidden
@RestController
@Profile("dev")
@RequestMapping("/api/dev/email-preview")
public class DevEmailPreviewController {

    private final String baseUrl;

    public DevEmailPreviewController(@Value("${app.frontend.base-url:http://localhost:5173}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @GetMapping(value = "/{kind}", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public String html(@PathVariable EmailKind kind, @RequestParam(defaultValue = "pickup") String variant) {
        return render(kind, variant).html();
    }

    @GetMapping(value = "/{kind}", params = "format=text", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public String text(@PathVariable EmailKind kind, @RequestParam(defaultValue = "pickup") String variant) {
        return render(kind, variant).text();
    }

    private EmailMessage render(EmailKind kind, String variant) {
        return MailTemplates.render(kind, MailSamples.sample("pembeli@example.com", baseUrl, !variant.equals("delivery"), kind != EmailKind.ORDER_CREATED));
    }
}
