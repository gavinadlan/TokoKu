package com.tokoku.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Produk")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // page, size, sort (mis. sort=price,desc) dibaca otomatis ke Pageable
    @Operation(summary = "Daftar produk aktif (cari q, filter category, pagination, sort)")
    @SecurityRequirements
    @GetMapping
    public Page<ProductResponse> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) Long category,
            @PageableDefault(size = 20, sort = "id") @ParameterObject Pageable pageable) {
        return service.search(q, category, pageable);
    }

    @Operation(summary = "Detail produk aktif")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Tambah produk (ADMIN)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Ubah produk (ADMIN)")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Atur stok produk (ADMIN)")
    @PatchMapping("/{id}/stock")
    public ProductResponse updateStock(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return service.updateStock(id, request.stock());
    }

    @Operation(summary = "Nonaktifkan produk, baris tidak dihapus (ADMIN)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
