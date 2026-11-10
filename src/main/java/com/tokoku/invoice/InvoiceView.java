package com.tokoku.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.tokoku.order.FulfillmentType;

/** Data yang dicetak di invoice (sudah dirangkum dari order, pembayaran, pembeli, dan pengaturan toko). */
public record InvoiceView(
        String number, String orderNumber, Instant issuedAt, Instant paidAt, String paymentMethod,
        String storeName, String storeAddress, String storePhone, String storeEmail, String taxId, String taxNote,
        String openingHours, String footer,
        String buyerName, String buyerPhone, String buyerEmail, String recipientName,
        FulfillmentType fulfillmentType, String shippingAddress, String courier, String trackingNumber, String pickupCode,
        List<Line> lines, BigDecimal subtotal, BigDecimal shippingFee, BigDecimal total) {

    public record Line(String name, int quantity, BigDecimal unitPrice, BigDecimal amount) {}
}
