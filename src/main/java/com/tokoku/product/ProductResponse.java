package com.tokoku.product;

import java.math.BigDecimal;
import java.util.List;

/** imageUrl = foto utama ukuran kartu (kompatibilitas); daftar lengkap ada di images. */
public record ProductResponse(Long id, Long categoryId, String categoryName, String name,
        String description, BigDecimal price, int stock, boolean active, String imageUrl, List<ProductImageResponse> images) {

    static ProductResponse from(Product p, ProductImageUrls urls) {
        List<ProductImageResponse> images = p.getImages().stream().map(urls::toResponse).toList();
        String primaryCard = images.stream().filter(ProductImageResponse::primary).map(ProductImageResponse::cardUrl).findFirst().orElse(null);
        return new ProductResponse(p.getId(), p.getCategory().getId(), p.getCategory().getName(),
                p.getName(), p.getDescription(), p.getPrice(), p.getStock(), p.isActive(), primaryCard, images);
    }
}
