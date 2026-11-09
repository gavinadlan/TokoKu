package com.tokoku.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

// Tambahan di luar tabel endpoint PRD: dibutuhkan agar filter kategori punya data
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Kategori")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @Operation(summary = "Daftar kategori")
    @SecurityRequirements
    @GetMapping
    public List<CategoryResponse> list() {
        return service.list();
    }

    @Operation(summary = "Tambah kategori (ADMIN)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Ubah nama kategori (ADMIN)")
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Hapus kategori; 409 jika masih punya produk (ADMIN)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
