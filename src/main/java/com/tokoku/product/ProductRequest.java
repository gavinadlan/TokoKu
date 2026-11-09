package com.tokoku.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

// DTO: bentuk data di API dipisah dari entity supaya skema DB tidak bocor dan validasi terpusat
public record ProductRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Min(0) int stock,
        // URL gambar diisi manual (penyimpanan file belum ada); hanya http/https
        @Size(max = 500) @Pattern(regexp = "^(https?://\\S+)?$", message = "must be an http(s) URL") String imageUrl) {}
