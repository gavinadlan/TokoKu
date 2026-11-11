package com.tokoku.notification;

import java.math.BigDecimal;
import java.util.List;

/**
 * Semua nilai yang masuk template. Hanya data pesanan ini, pemiliknya, dan identitas toko; semuanya dianggap
 * tidak tepercaya (di-escape saat dirender).
 */
public record MailData(Long orderId, String recipientName, String to, String dateLabel, boolean pickup,
        List<Line> items, BigDecimal subtotal, BigDecimal shippingFee, BigDecimal total,
        String shippingName, String shippingAddress, String pickupCode, String courier, String trackingNumber,
        String invoiceNumber, boolean invoiceAttached, String orderUrl,
        String storeName, String storeAddress, String storePhone, String storeEmail, String openingHours, String pickupInstructions) {

    public record Line(String name, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
}
