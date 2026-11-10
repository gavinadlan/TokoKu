package com.tokoku.product;

public record ProductImageResponse(Long id, boolean primary, int position, Integer width, Integer height,
        String thumbnailUrl, String cardUrl, String detailUrl, String largeUrl) {}
