package com.tokoku.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService service;

    public AdminProductController(ProductService service) {
        this.service = service;
    }

    // Seperti daftar publik, tetapi termasuk produk nonaktif; filter active=true|false opsional
    @GetMapping
    public Page<ProductResponse> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.searchAll(q, category, active, pageable);
    }
}
