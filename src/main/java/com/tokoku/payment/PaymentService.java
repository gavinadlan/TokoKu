package com.tokoku.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tokoku.auth.User;
import com.tokoku.auth.UserRepository;
import com.tokoku.common.ConflictException;
import com.tokoku.common.InvalidSignatureException;
import com.tokoku.common.NotFoundException;
import com.tokoku.order.FulfillmentType;
import com.tokoku.order.Order;
import com.tokoku.order.PickupCodes;
import com.tokoku.order.OrderRepository;
import com.tokoku.order.OrderService;
import com.tokoku.order.OrderStatus;
import com.tokoku.payment.PaymentDtos.PayResponse;
import com.tokoku.payment.PaymentGateway.Item;
import com.tokoku.payment.PaymentGateway.Notification;
import com.tokoku.payment.PaymentGateway.PaymentRequest;
import com.tokoku.payment.PaymentGateway.PaymentSession;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final UserRepository users;
    private final PaymentGateway gateway;
    private final int expiryMinutes;
    private final com.tokoku.invoice.InvoiceNumberService invoiceNumbers;

    public PaymentService(PaymentRepository payments, OrderRepository orders, OrderService orderService,
            UserRepository users, PaymentGateway gateway, @Value("${app.order.expiry-minutes:60}") int expiryMinutes,
            com.tokoku.invoice.InvoiceNumberService invoiceNumbers) {
        this.invoiceNumbers = invoiceNumbers;
        this.payments = payments;
        this.orders = orders;
        this.orderService = orderService;
        this.users = users;
        this.gateway = gateway;
        this.expiryMinutes = expiryMinutes;
    }

    /** Memulai pembayaran. Idempoten: jika sudah ada pembayaran PENDING untuk order ini, itu yang dikembalikan. */
    @Transactional(noRollbackFor = ConflictException.class) // pembatalan karena kedaluwarsa harus tetap tersimpan
    public PayResponse pay(Long userId, Long orderId) {
        Order order = orders.findOwnedForUpdate(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " not found"));
        orderService.cancelIfOverdue(order);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Order is " + order.getStatus() + ", only PENDING_PAYMENT can be paid");
        }
        Payment existing = payments.findFirstByOrderIdAndStatus(orderId, PaymentStatus.PENDING).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        String ref = "TKU-" + orderId + "-" + UUID.randomUUID().toString().substring(0, 8); // unik per percobaan
        List<Item> items = new java.util.ArrayList<>(order.getItems().stream()
                .map(i -> new Item("P" + i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity())).toList());
        if (order.getShippingFee().signum() > 0) {
            // Ongkir ikut sebagai baris sendiri agar jumlah item_details sama dengan gross_amount
            items.add(new Item("SHIPPING", "Biaya kirim", order.getShippingFee(), 1));
        }
        PaymentSession session = gateway.createPayment(new PaymentRequest(ref, order.getTotalAmount(), items,
                order.getShippingName(), user.getEmail(), order.getShippingPhone(), remainingMinutes(order)));
        Payment payment = payments.save(new Payment(orderId, ref, order.getTotalAmount(), session.token(), session.redirectUrl()));
        return toResponse(payment);
    }

    /** Webhook: signature dulu, lalu jalur transisi yang sama dengan rekonsiliasi. */
    @Transactional
    public void handleNotification(Notification n) {
        if (n.orderId() == null || n.signatureKey() == null || !gateway.verifySignature(n)) {
            throw new InvalidSignatureException("Invalid signature");
        }
        applyStatus(n, null);
    }

    /**
     * SATU jalur transisi untuk webhook dan rekonsiliasi (Status API). Urutan penjagaan: payment dikenal, milik order
     * yang diharapkan (bila diberikan), nominal cocok, idempotensi (payment sudah tidak PENDING = tidak berubah apa pun).
     * Semua perubahan (payment, order, stok) dalam satu transaksi.
     *
     * @param expectedOrderId null untuk webhook; untuk rekonsiliasi = order yang ditanyakan, payment order lain ditolak
     */
    @Transactional
    public void applyStatus(Notification n, Long expectedOrderId) {
        if (n.orderId() == null) {
            throw new IllegalArgumentException("Missing order_id");
        }
        // Urutan kunci SELALU order lalu payment (sama seperti pay/cancel/expire) agar tidak terjadi deadlock
        Payment known = payments.findByProviderRef(n.orderId())
                .orElseThrow(() -> new NotFoundException("Unknown payment reference"));
        if (expectedOrderId != null && !expectedOrderId.equals(known.getOrderId())) {
            throw new NotFoundException("Unknown payment reference");
        }
        Order order = orders.findByIdForUpdate(known.getOrderId()).orElseThrow();
        Payment payment = payments.findByProviderRefForUpdate(n.orderId()).orElseThrow();
        if (n.grossAmount() == null || payment.getAmount().compareTo(parseAmount(n.grossAmount())) != 0) {
            throw new IllegalArgumentException("Amount does not match the order total");
        }
        if (!payment.isPending()) {
            return; // webhook ulang/terlambat, atau webhook dan rekonsiliasi datang bersamaan: tidak mengubah apa pun
        }
        String status = n.transactionStatus() == null ? "" : n.transactionStatus();
        boolean success = status.equals("settlement")
                || (status.equals("capture") && (n.fraudStatus() == null || n.fraudStatus().equals("accept")));
        if (success) {
            onSuccess(payment, order, n.paymentType());
        } else if (status.equals("expire")) {
            onFailure(payment, order, PaymentStatus.EXPIRED);
        } else if (status.equals("deny") || status.equals("cancel") || status.equals("failure")) {
            onFailure(payment, order, PaymentStatus.FAILED);
        }
        // pending / authorize / capture+challenge / refund, dll: abaikan, tidak ada perubahan
    }

    private void onSuccess(Payment payment, Order order, String paymentType) {
        payment.markPaid(paymentType);
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.transitionTo(OrderStatus.PAID);
            order.assignInvoice(invoiceNumbers.next(), Instant.now()); // idempoten: webhook ganda berhenti di cek status payment
            if (order.getFulfillmentType() == FulfillmentType.PICKUP && order.getPickupCode() == null) {
                order.assignPickupCode(PickupCodes.generate()); // kode ambil dibuat saat order dibayar
            }
            orderService.notifyCustomer(order, com.tokoku.notification.EmailKind.PAID); // webhook ganda berhenti di cek payment.isPending
        } else {
            // Uang masuk setelah order dibatalkan/kedaluwarsa: butuh penanganan manual (refund)
            log.warn("Payment {} settled but order {} is {}; manual refund needed", payment.getProviderRef(), order.getId(), order.getStatus());
        }
    }

    private void onFailure(Payment payment, Order order, PaymentStatus ended) {
        payment.markEnded(ended);
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            orderService.cancelAndRestock(order); // terakhir: update stok melakukan flush
        }
    }

    // Batas bayar di Midtrans = sisa umur order, supaya pembayaran tidak bisa masuk setelah order kedaluwarsa
    private int remainingMinutes(Order order) {
        long used = java.time.Duration.between(order.getCreatedAt(), java.time.Instant.now()).toMinutes();
        return (int) Math.max(1, expiryMinutes - used);
    }

    private static BigDecimal parseAmount(String s) {
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid gross_amount");
        }
    }

    private PayResponse toResponse(Payment p) {
        return new PayResponse(p.getId(), p.getOrderId(), p.getProviderRef(), p.getStatus(), p.getAmount(),
                p.getToken(), p.getRedirectUrl(), gateway.clientKey());
    }
}
