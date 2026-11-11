package com.tokoku.payment;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.tokoku.common.KeyRateLimiter;
import com.tokoku.common.NotFoundException;
import com.tokoku.common.PaymentGatewayException;
import com.tokoku.order.Order;
import com.tokoku.order.OrderDtos.OrderResponse;
import com.tokoku.order.OrderRepository;
import com.tokoku.order.OrderService;
import com.tokoku.order.OrderStatus;
import com.tokoku.payment.PaymentGateway.Notification;

/**
 * Rekonsiliasi lewat Status API provider, agar status pesanan tidak bergantung pada webhook (yang tidak bisa menjangkau
 * localhost dan bisa terlambat di produksi). Panggilan jaringan dilakukan DI LUAR transaksi; hasilnya diterapkan lewat
 * PaymentService.applyStatus, jalur yang sama dengan webhook.
 */
@Service
public class PaymentReconciler {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciler.class);

    private final PaymentGateway gateway;
    private final PaymentService payments;
    private final PaymentRepository paymentRepo;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final KeyRateLimiter limiter;
    private final int batchSize;

    public PaymentReconciler(PaymentGateway gateway, PaymentService payments, PaymentRepository paymentRepo, OrderRepository orders,
            OrderService orderService, @Value("${app.payment.refresh-per-minute:20}") int perMinute,
            @Value("${app.payment.reconcile.batch-size:20}") int batchSize) {
        this.gateway = gateway;
        this.payments = payments;
        this.paymentRepo = paymentRepo;
        this.orders = orders;
        this.orderService = orderService;
        this.limiter = new KeyRateLimiter(perMinute, Duration.ofMinutes(1), "Too many payment status checks, try again shortly");
        this.batchSize = batchSize;
    }

    /** Pembeli pemilik pesanan meminta pemeriksaan status. Order orang lain = 404. Gangguan provider = status saat ini. */
    public OrderResponse refreshForOwner(Long userId, Long orderId) {
        limiter.check(String.valueOf(userId));
        Order order = orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            paymentRepo.findFirstByOrderIdAndStatus(orderId, PaymentStatus.PENDING).ifPresent(p -> reconcile(p, orderId, true));
        }
        return orderService.getMine(userId, orderId); // juga menerapkan kedaluwarsa "malas"
    }

    /** Jaring pengaman: payment PENDING berumur 2 menit sampai 24 jam, maksimal batchSize panggilan per putaran. */
    @Scheduled(fixedDelayString = "${app.payment.reconcile.interval-ms:300000}", initialDelayString = "${app.payment.reconcile.initial-delay-ms:60000}")
    public void reconcileRecent() {
        Instant now = Instant.now();
        List<Payment> pending = paymentRepo.findByStatusAndCreatedAtBetweenOrderByCreatedAtAsc(PaymentStatus.PENDING,
                now.minus(Duration.ofHours(24)), now.minus(Duration.ofMinutes(2)), PageRequest.of(0, batchSize));
        int changed = 0;
        for (Payment p : pending) {
            if (reconcile(p, p.getOrderId(), false)) changed++;
        }
        if (changed > 0) log.info("Rekonsiliasi pembayaran: {} dari {} payment PENDING berubah", changed, pending.size());
    }

    /** true bila provider melaporkan status yang diterapkan (perubahan nyata ditentukan applyStatus). */
    boolean reconcile(Payment p, Long orderId, boolean userRequested) {
        Optional<Notification> status;
        try {
            status = gateway.fetchStatus(p.getProviderRef());
        } catch (PaymentGatewayException e) {
            log.warn("Status pembayaran {} belum bisa diperiksa: {}", p.getProviderRef(), e.getMessage());
            return false;
        }
        if (status.isEmpty()) return false; // pembeli belum memilih metode bayar
        try {
            payments.applyStatus(status.get(), orderId);
            return true;
        } catch (RuntimeException e) {
            // nominal tidak cocok / referensi asing: tolak, jangan ubah apa pun
            log.warn("Status pembayaran {} ditolak: {}", p.getProviderRef(), e.getMessage());
            if (userRequested && e instanceof IllegalArgumentException) throw e;
            return false;
        }
    }
}
