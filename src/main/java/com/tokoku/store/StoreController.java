package com.tokoku.store;

import org.springframework.web.bind.annotation.*;

import com.tokoku.store.StoreDtos.AdminStoreResponse;
import com.tokoku.store.StoreDtos.PublicStoreResponse;
import com.tokoku.store.StoreDtos.StoreSettingsRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Tag(name = "Toko")
public class StoreController {

    private final StoreService service;

    public StoreController(StoreService service) {
        this.service = service;
    }

    @Operation(summary = "Info toko untuk publik (alamat, jam buka, pengambilan, biaya kirim)")
    @SecurityRequirements
    @GetMapping("/api/store")
    public PublicStoreResponse publicStore() {
        return service.publicView();
    }

    @Operation(summary = "Pengaturan toko lengkap (ADMIN)")
    @GetMapping("/api/admin/store-settings")
    public AdminStoreResponse adminStore() {
        return service.adminView();
    }

    @Operation(summary = "Ubah pengaturan toko (ADMIN)")
    @PutMapping("/api/admin/store-settings")
    public AdminStoreResponse update(@Valid @RequestBody StoreSettingsRequest request) {
        return service.update(request);
    }
}
