package com.tokoku.payment;

import java.math.BigDecimal;
import java.util.List;

// Penyedia pembayaran dibungkus interface: Midtrans untuk sungguhan, Fake untuk test/CI tanpa jaringan
public interface PaymentGateway {

    PaymentSession createPayment(PaymentRequest request);

    /** True jika notifikasi benar-benar berasal dari provider (signature cocok). */
    boolean verifySignature(Notification notification);

    /**
     * Status transaksi menurut provider (rekonsiliasi tanpa webhook). Kosong = provider belum mengenal transaksi ini
     * (pembeli belum memilih metode bayar). Hasil dipakai lewat jalur transisi yang sama dengan webhook.
     */
    java.util.Optional<Notification> fetchStatus(String providerRef);

    /** Client key publik untuk frontend; null jika provider tidak memakainya. */
    default String clientKey() { return null; }

    record Item(String id, String name, BigDecimal price, int quantity) {}

    record PaymentRequest(String providerRef, BigDecimal amount, List<Item> items, String customerName,
            String customerEmail, String customerPhone, int expiryMinutes) {}

    record PaymentSession(String token, String redirectUrl) {}

    record Notification(String orderId, String statusCode, String grossAmount, String signatureKey,
            String transactionStatus, String fraudStatus, String transactionId, String paymentType) {}
}
