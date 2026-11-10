package com.tokoku.product;

import org.springframework.stereotype.Component;

import com.tokoku.image.ImageSize;
import com.tokoku.image.ImageStorage;

/** Menerjemahkan gambar tersimpan menjadi URL tiga ukuran bernama. */
@Component
public class ProductImageUrls {

    private final ImageStorage storage;

    public ProductImageUrls(ImageStorage storage) {
        this.storage = storage;
    }

    ProductImageResponse toResponse(ProductImage i) {
        return new ProductImageResponse(i.getId(), i.isPrimary(), i.getPosition(), i.getWidth(), i.getHeight(),
                storage.variantUrl(i.getUrl(), i.getPublicId(), ImageSize.THUMBNAIL),
                storage.variantUrl(i.getUrl(), i.getPublicId(), ImageSize.CARD),
                storage.variantUrl(i.getUrl(), i.getPublicId(), ImageSize.DETAIL));
    }

    /** Thumbnail foto utama, atau null jika produk belum punya foto. */
    public String primaryThumbnail(Product p) {
        return p.getImages().stream().filter(ProductImage::isPrimary).findFirst()
                .map(i -> storage.variantUrl(i.getUrl(), i.getPublicId(), ImageSize.THUMBNAIL)).orElse(null);
    }
}
