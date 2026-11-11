package com.tokoku.product;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public final class ProductImageDtos {

    private ProductImageDtos() {}

    /** Hasil unggah yang dilaporkan browser; server memverifikasi signature dan membangun URL sendiri. */
    public record RegisterImageRequest(@NotBlank String publicId, @Min(1) long version, @NotBlank String signature,
            Integer width, Integer height, @NotBlank String format, @Min(1) long bytes) {}

    public record ReorderRequest(@NotEmpty List<@NotNull Long> imageIds) {}
}
