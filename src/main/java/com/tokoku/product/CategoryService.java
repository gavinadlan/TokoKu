package com.tokoku.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tokoku.common.ConflictException;
import com.tokoku.common.NotFoundException;

@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final ProductRepository products;

    public CategoryService(CategoryRepository repository, ProductRepository products) {
        this.repository = repository;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return repository.findAll().stream().map(c -> new CategoryResponse(c.getId(), c.getName())).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category c = repository.save(new Category(request.name().trim()));
        return new CategoryResponse(c.getId(), c.getName());
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category c = repository.findById(id).orElseThrow(() -> new NotFoundException("Category " + id + " not found"));
        c.rename(request.name().trim());
        repository.flush(); // agar nama ganda ketahuan sekarang (409), bukan saat commit
        return new CategoryResponse(c.getId(), c.getName());
    }

    // Kategori yang masih punya produk (aktif atau tidak) tidak boleh dihapus
    @Transactional
    public void delete(Long id) {
        Category c = repository.findById(id).orElseThrow(() -> new NotFoundException("Category " + id + " not found"));
        if (products.existsByCategoryId(id)) {
            throw new ConflictException("Category still has products");
        }
        repository.delete(c);
    }
}
