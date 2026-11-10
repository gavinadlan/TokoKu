package com.tokoku.invoice;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Order")
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @Operation(summary = "Invoice PDF order sendiri (hanya order yang sudah dibayar; 409 bila belum)")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    @GetMapping("/api/orders/{id}/invoice")
    public ResponseEntity<byte[]> mine(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return pdf(service.forOwner(Long.valueOf(jwt.getSubject()), id), id);
    }

    @Operation(summary = "Invoice PDF order apa pun (ADMIN)")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    @GetMapping("/api/admin/orders/{id}/invoice")
    public ResponseEntity<byte[]> admin(@PathVariable Long id) {
        return pdf(service.forAdmin(id), id);
    }

    private ResponseEntity<byte[]> pdf(byte[] bytes, Long orderId) {
        String number = service.numberOf(orderId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename((number == null ? "invoice" : number) + ".pdf").build().toString())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(bytes);
    }
}
