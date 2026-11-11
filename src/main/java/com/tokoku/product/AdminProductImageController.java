package com.tokoku.product;

import org.springframework.web.bind.annotation.*;

import com.tokoku.image.UploadSignature;
import com.tokoku.product.ProductImageDtos.RegisterImageRequest;
import com.tokoku.product.ProductImageDtos.ReorderRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
public class AdminProductImageController {

    private final ProductImageService service;

    public AdminProductImageController(ProductImageService service) {
        this.service = service;
    }

    @Operation(summary = "Tanda tangan unggah langsung ke penyimpanan gambar (ADMIN)")
    @PostMapping("/uploads/sign")
    public UploadSignature sign() {
        return service.signUpload();
    }

    @Operation(summary = "Daftarkan foto yang sudah diunggah; signature diverifikasi, maksimal 6 foto (ADMIN)")
    @PostMapping("/products/{id}/images")
    public ProductResponse register(@PathVariable Long id, @Valid @RequestBody RegisterImageRequest request) {
        return service.register(id, request);
    }

    @Operation(summary = "Ubah urutan foto: daftar semua id foto sesuai urutan baru (ADMIN)")
    @PutMapping("/products/{id}/images/order")
    public ProductResponse reorder(@PathVariable Long id, @Valid @RequestBody ReorderRequest request) {
        return service.reorder(id, request.imageIds());
    }

    @Operation(summary = "Jadikan foto utama (ADMIN)")
    @PatchMapping("/products/{id}/images/{imageId}/primary")
    public ProductResponse primary(@PathVariable Long id, @PathVariable Long imageId) {
        return service.makePrimary(id, imageId);
    }

    @Operation(summary = "Hapus foto; penghapusan di penyimpanan best-effort (ADMIN)")
    @DeleteMapping("/products/{id}/images/{imageId}")
    public ProductResponse delete(@PathVariable Long id, @PathVariable Long imageId) {
        return service.delete(id, imageId);
    }
}
