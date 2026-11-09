package com.tokoku.cart;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class CartDtos {

    private CartDtos() {}

    public record AddItemRequest(@NotNull Long productId, @Min(1) @Max(1000) int quantity) {}

    public record UpdateItemRequest(@Min(1) @Max(1000) int quantity) {}

    public record CartItemResponse(Long id, Long productId, String productName, String imageUrl, BigDecimal unitPrice,
            int quantity, BigDecimal subtotal, int availableStock) {}

    public record CartResponse(List<CartItemResponse> items, int totalQuantity, BigDecimal total) {}
}
