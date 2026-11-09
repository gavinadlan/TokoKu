package com.tokoku.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.tokoku.payment.PaymentDtos.PayResponse;
import com.tokoku.payment.PaymentDtos.WebhookResult;

@RestController
@Tag(name = "Pembayaran")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @Operation(summary = "Mulai pembayaran Midtrans Snap; idempoten (mengembalikan token dan redirectUrl)")
    @PostMapping("/api/orders/{id}/pay")
    public PayResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.pay(Long.valueOf(jwt.getSubject()), id);
    }

    // Publik (provider tidak punya token kita); keamanan = verifikasi signature di service
    @Operation(summary = "Webhook notifikasi Midtrans; publik tetapi wajib signature valid")
    @SecurityRequirements
    @PostMapping("/api/payments/webhook")
    public WebhookResult webhook(@RequestBody Map<String, Object> body) {
        service.handleNotification(PaymentDtos.toNotification(body));
        return new WebhookResult("ok");
    }
}
