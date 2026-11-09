package com.tokoku.payment;

import java.math.BigDecimal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import com.tokoku.common.NotFoundException;
import com.tokoku.order.OrderRepository;
import com.tokoku.payment.PaymentDtos.SimulationResult;

// Hanya ada saat provider=fake DAN profil bukan prod: simulasi webhook untuk dev/test tanpa Midtrans
@RestController
@Profile("!prod")
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "fake")
@RequestMapping("/api/dev/payments")
public class DevPaymentController {

    private final PaymentService service;
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final FakePaymentGateway gateway;

    public DevPaymentController(PaymentService service, PaymentRepository payments, OrderRepository orders,
            FakePaymentGateway gateway) {
        this.service = service;
        this.payments = payments;
        this.orders = orders;
        this.gateway = gateway;
    }

    @PostMapping("/{ref}/simulate")
    public SimulationResult simulate(@PathVariable String ref, @RequestParam String status) {
        if (!java.util.Set.of("settlement", "expire", "deny").contains(status)) {
            throw new IllegalArgumentException("status must be settlement, expire or deny");
        }
        Payment p = payments.findByProviderRef(ref).orElseThrow(() -> new NotFoundException("Unknown payment reference"));
        String gross = p.getAmount().setScale(2).toPlainString(); // bentuk "60000.00" seperti notifikasi Midtrans
        service.handleNotification(gateway.signedNotification(ref, "200", gross, status));
        Payment after = payments.findByProviderRef(ref).orElseThrow();
        return new SimulationResult(ref, after.getStatus(), orders.findById(after.getOrderId()).orElseThrow().getStatus().name());
    }
}
