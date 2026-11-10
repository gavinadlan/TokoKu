package com.tokoku.notification;

import java.math.BigDecimal;
import java.util.List;

/** Semua nilai yang masuk template. Dibangun dari order + pengaturan toko; semuanya dianggap tidak tepercaya (di-escape). */
public record MailData(Long orderId, String recipientName, String to, BigDecimal total, boolean pickup,
        List<Line> items, String pickupCode, String courier, String trackingNumber, String orderUrl,
        String storeName, String storeAddress, String openingHours, String pickupInstructions) {

    public record Line(String name, int quantity) {}
}
