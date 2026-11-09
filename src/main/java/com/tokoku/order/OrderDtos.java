package com.tokoku.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class OrderDtos {

    private OrderDtos() {}

    public record CheckoutRequest(
            @NotBlank @Size(max = 100) String shippingName,
            @NotBlank @Size(max = 30) String shippingPhone,
            @NotBlank @Size(max = 500) String shippingAddress) {}

    public record StatusRequest(@NotNull OrderStatus status) {}

    public record OrderItemResponse(Long productId, String productName, int quantity, BigDecimal unitPrice,
            BigDecimal subtotal) {}

    public record OrderResponse(Long id, Long userId, OrderStatus status, BigDecimal totalAmount, String shippingName,
            String shippingPhone, String shippingAddress, Instant createdAt, List<OrderItemResponse> items) {

        static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getUserId(), o.getStatus(), o.getTotalAmount(), o.getShippingName(),
                    o.getShippingPhone(), o.getShippingAddress(), o.getCreatedAt(),
                    o.getItems().stream().map(i -> new OrderItemResponse(i.getProductId(), i.getProductName(),
                            i.getQuantity(), i.getUnitPrice(), i.getSubtotal())).toList());
        }
    }
}
