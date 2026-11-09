package com.tokoku.payment;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.tokoku.payment.PaymentDtos.PayResponse;
import com.tokoku.payment.PaymentDtos.WebhookResult;

@RestController
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/api/orders/{id}/pay")
    public PayResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.pay(Long.valueOf(jwt.getSubject()), id);
    }

    // Publik (provider tidak punya token kita); keamanan = verifikasi signature di service
    @PostMapping("/api/payments/webhook")
    public WebhookResult webhook(@RequestBody Map<String, Object> body) {
        service.handleNotification(PaymentDtos.toNotification(body));
        return new WebhookResult("ok");
    }
}
