package com.tokoku.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tokoku.common.NotFoundException;

@Service
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductImageUrls urls;

    public ProductService(ProductRepository products, CategoryRepository categories, ProductImageUrls urls) {
        this.products = products;
        this.categories = categories;
        this.urls = urls;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String q, Long categoryId, Pageable pageable) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return products.search(pattern, categoryId, pageable).map(p -> ProductResponse.from(p, urls));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchAll(String q, Long categoryId, Boolean active, Pageable pageable) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return products.searchAll(pattern, categoryId, active, pageable).map(p -> ProductResponse.from(p, urls));
    }

    @Transactional(readOnly = true)
    public ProductResponse getAny(Long id) {
        return ProductResponse.from(products.findById(id).orElseThrow(() -> notFound(id)), urls);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(products.findByIdAndActiveTrue(id).orElseThrow(() -> notFound(id)), urls);
    }

    // @Transactional: semua langkah dalam method = satu transaksi DB, commit bersama atau rollback bersama
    @Transactional
    public ProductResponse create(ProductRequest r) {
        Product p = new Product(category(r.categoryId()), r.name(), r.description(), r.price(), r.stock());
        return ProductResponse.from(products.save(p), urls);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest r) {
        Product p = products.findById(id).orElseThrow(() -> notFound(id));
        p.update(category(r.categoryId()), r.name(), r.description(), r.price());
        p.setStock(r.stock());
        return ProductResponse.from(p, urls); // entity managed: perubahan otomatis di-flush saat commit
    }

    @Transactional
    public ProductResponse updateStock(Long id, int stock) {
        Product p = products.findById(id).orElseThrow(() -> notFound(id));
        p.setStock(stock);
        return ProductResponse.from(p, urls);
    }

    // Soft delete: baris tetap ada (order lama nanti masih merujuk produk ini)
    @Transactional
    public void deactivate(Long id) {
        products.findById(id).orElseThrow(() -> notFound(id)).deactivate();
    }

    private Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Category " + id + " not found"));
    }

    private NotFoundException notFound(Long id) {
        return new NotFoundException("Product " + id + " not found");
    }
}
