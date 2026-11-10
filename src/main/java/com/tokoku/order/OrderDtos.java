package com.tokoku.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class OrderDtos {

    private OrderDtos() {}

    /** fulfillmentType kosong = DELIVERY. Alamat wajib hanya untuk DELIVERY; PICKUP cukup nama dan telepon. */
    public record CheckoutRequest(
            FulfillmentType fulfillmentType,
            @NotBlank @Size(max = 100) String shippingName,
            @NotBlank @Size(max = 30) String shippingPhone,
            @Size(max = 500) String shippingAddress) {}

    /** courier dan trackingNumber wajib untuk SHIPPED; pickupCode wajib untuk menyelesaikan order PICKUP. */
    public record StatusRequest(@NotNull OrderStatus status, @Size(max = 50) String courier,
            @Size(max = 60) String trackingNumber, @Size(max = 20) String pickupCode) {}

    public record OrderItemResponse(Long productId, String productName, int quantity, BigDecimal unitPrice,
            BigDecimal subtotal) {}

    public record OrderResponse(Long id, Long userId, OrderStatus status, FulfillmentType fulfillmentType,
            BigDecimal subtotal, BigDecimal shippingFee, BigDecimal totalAmount, String shippingName,
            String shippingPhone, String shippingAddress, String courier, String trackingNumber, String pickupCode,
            Instant pickupReadyAt, Instant createdAt, List<OrderItemResponse> items) {

        static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getUserId(), o.getStatus(), o.getFulfillmentType(), o.getSubtotal(),
                    o.getShippingFee(), o.getTotalAmount(), o.getShippingName(),
                    o.getShippingPhone(), o.getShippingAddress(), o.getCourier(), o.getTrackingNumber(), o.getPickupCode(),
                    o.getPickupReadyAt(), o.getCreatedAt(),
                    o.getItems().stream().map(i -> new OrderItemResponse(i.getProductId(), i.getProductName(),
                            i.getQuantity(), i.getUnitPrice(), i.getSubtotal())).toList());
        }
    }
}
