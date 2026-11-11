package com.tokoku.payment;

import java.math.BigDecimal;
import java.util.Map;

public final class PaymentDtos {

    private PaymentDtos() {}

    public record PayResponse(Long paymentId, Long orderId, String providerRef, PaymentStatus status, BigDecimal amount,
            String token, String redirectUrl, String clientKey) {}

    public record WebhookResult(String status) {}

    public record SimulationResult(String providerRef, PaymentStatus paymentStatus, String orderStatus) {}

    /** Webhook Midtrans memuat banyak field; kita ambil yang dipakai saja dari JSON bebas. */
    static PaymentGateway.Notification toNotification(Map<String, Object> body) {
        return new PaymentGateway.Notification(str(body, "order_id"), str(body, "status_code"), str(body, "gross_amount"),
                str(body, "signature_key"), str(body, "transaction_status"), str(body, "fraud_status"), str(body, "transaction_id"), str(body, "payment_type"));
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : String.valueOf(v);
    }
}
