package com.tokoku.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.tokoku.order.OrderDtos.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @Operation(summary = "Checkout dari keranjang: stok dikurangi atomik, order PENDING_PAYMENT")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest request) {
        return service.checkout(Long.valueOf(jwt.getSubject()), request);
    }

    @Operation(summary = "Riwayat order sendiri")
    @GetMapping
    public Page<OrderResponse> list(@AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listMine(Long.valueOf(jwt.getSubject()), pageable);
    }

    @Operation(summary = "Detail order sendiri (order orang lain = 404)")
    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getMine(Long.valueOf(jwt.getSubject()), id);
    }

    @Operation(summary = "Batalkan order yang belum dibayar; stok dikembalikan")
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cancelMine(Long.valueOf(jwt.getSubject()), id);
    }
}
