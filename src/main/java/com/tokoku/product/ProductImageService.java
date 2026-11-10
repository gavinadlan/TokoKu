package com.tokoku.product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.tokoku.common.ConflictException;
import com.tokoku.common.NotFoundException;
import com.tokoku.image.ImageStorage;
import com.tokoku.image.UploadSignature;
import com.tokoku.product.ProductImageDtos.RegisterImageRequest;

@Service
public class ProductImageService {

    public static final int MAX_IMAGES = 6;
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> FORMATS = Set.of("jpg", "jpeg", "png", "webp");

    private final ProductRepository products;
    private final ProductImageRepository images;
    private final ImageStorage storage;
    private final ProductImageUrls urls;

    public ProductImageService(ProductRepository products, ProductImageRepository images, ImageStorage storage, ProductImageUrls urls) {
        this.products = products;
        this.images = images;
        this.storage = storage;
        this.urls = urls;
    }

    public UploadSignature signUpload() {
        return storage.signUpload();
    }

    /** Mendaftarkan hasil unggah. Signature diverifikasi dulu; URL dibangun server, bukan diterima dari klien. */
    @Transactional
    public ProductResponse register(Long productId, RegisterImageRequest r) {
        Product p = products.findById(productId).orElseThrow(() -> new NotFoundException("Product " + productId + " not found"));
        if (!r.publicId().startsWith(ImageStorage.FOLDER + "/") || !storage.verifyUploadResponse(r.publicId(), r.version(), r.signature())) {
            throw new IllegalArgumentException("Invalid upload signature");
        }
        String format = r.format().toLowerCase(Locale.ROOT);
        if (!FORMATS.contains(format) || r.bytes() > MAX_BYTES) {
            storage.delete(r.publicId()); // unggahan sah tetapi melanggar aturan: bersihkan asetnya
            throw new IllegalArgumentException(!FORMATS.contains(format) ? "Only jpg, png and webp images are allowed" : "Image must be at most 5 MB");
        }
        if (p.getImages().size() >= MAX_IMAGES) {
            storage.delete(r.publicId());
            throw new ConflictException("A product can have at most " + MAX_IMAGES + " images");
        }
        if (images.existsByPublicId(r.publicId())) {
            throw new ConflictException("Image already registered");
        }
        int position = p.getImages().stream().mapToInt(ProductImage::getPosition).max().orElse(-1) + 1;
        boolean primary = p.getImages().isEmpty();
        p.getImages().add(new ProductImage(p, r.publicId(), storage.originalUrl(r.publicId(), r.version(), "jpeg".equals(format) ? "jpg" : format),
                r.width(), r.height(), position, primary));
        products.saveAndFlush(p);
        return ProductResponse.from(p, urls);
    }

    @Transactional
    public ProductResponse reorder(Long productId, List<Long> imageIds) {
        Product p = products.findById(productId).orElseThrow(() -> new NotFoundException("Product " + productId + " not found"));
        Set<Long> current = new HashSet<>();
        p.getImages().forEach(i -> current.add(i.getId()));
        if (imageIds.size() != current.size() || !current.equals(new HashSet<>(imageIds))) {
            throw new IllegalArgumentException("imageIds must list every image of the product exactly once");
        }
        for (int pos = 0; pos < imageIds.size(); pos++) {
            Long id = imageIds.get(pos);
            int newPos = pos;
            p.getImages().stream().filter(i -> i.getId().equals(id)).findFirst().orElseThrow().setPosition(newPos);
        }
        products.saveAndFlush(p);
        p.getImages().sort(java.util.Comparator.comparingInt(ProductImage::getPosition));
        return ProductResponse.from(p, urls);
    }

    @Transactional
    public ProductResponse makePrimary(Long productId, Long imageId) {
        Product p = products.findById(productId).orElseThrow(() -> new NotFoundException("Product " + productId + " not found"));
        if (p.getImages().stream().noneMatch(i -> i.getId().equals(imageId))) {
            throw new NotFoundException("Image " + imageId + " not found");
        }
        images.clearPrimary(productId);
        images.markPrimary(imageId);
        return ProductResponse.from(products.findById(productId).orElseThrow(), urls);
    }

    @Transactional
    public ProductResponse delete(Long productId, Long imageId) {
        Product p = products.findById(productId).orElseThrow(() -> new NotFoundException("Product " + productId + " not found"));
        ProductImage img = p.getImages().stream().filter(i -> i.getId().equals(imageId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Image " + imageId + " not found"));
        boolean wasPrimary = img.isPrimary();
        String publicId = img.getPublicId();
        p.getImages().remove(img); // orphanRemoval menghapus barisnya
        products.flush();           // hapus dulu sebelum foto utama baru ditentukan (indeks unik)
        List<ProductImage> rest = new ArrayList<>(p.getImages());
        for (int pos = 0; pos < rest.size(); pos++) rest.get(pos).setPosition(pos);
        if (wasPrimary && !rest.isEmpty()) rest.get(0).setPrimary(true);
        products.flush();
        if (publicId != null) {
            // Hapus di penyimpanan setelah commit; kegagalan tidak menggagalkan permintaan (best-effort)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { storage.delete(publicId); }
            });
        }
        return ProductResponse.from(p, urls);
    }
}
