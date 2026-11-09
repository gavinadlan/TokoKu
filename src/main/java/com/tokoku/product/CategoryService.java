package com.tokoku.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
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
}
