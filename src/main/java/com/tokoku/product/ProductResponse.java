package com.tokoku.product;

import java.math.BigDecimal;

public record ProductResponse(Long id, Long categoryId, String categoryName, String name,
        String description, BigDecimal price, int stock, boolean active, String imageUrl) {

    static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getCategory().getId(), p.getCategory().getName(),
                p.getName(), p.getDescription(), p.getPrice(), p.getStock(), p.isActive(), p.getImageUrl());
    }
}
