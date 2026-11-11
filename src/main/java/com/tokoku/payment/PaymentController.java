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
    private final PaymentReconciler reconciler;

    public PaymentController(PaymentService service, PaymentReconciler reconciler) {
        this.service = service;
        this.reconciler = reconciler;
    }

    @Operation(summary = "Mulai pembayaran Midtrans Snap; idempoten (mengembalikan token dan redirectUrl)")
    @PostMapping("/api/orders/{id}/pay")
    public PayResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.pay(Long.valueOf(jwt.getSubject()), id);
    }

    @Operation(summary = "Periksa status pembayaran ke provider (tanpa menunggu webhook); pemilik order saja, dibatasi per pengguna")
    @PostMapping("/api/orders/{id}/payment/refresh")
    public com.tokoku.order.OrderDtos.OrderResponse refresh(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return reconciler.refreshForOwner(Long.valueOf(jwt.getSubject()), id);
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
