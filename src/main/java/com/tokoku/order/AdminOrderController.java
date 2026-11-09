package com.tokoku.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.tokoku.order.OrderDtos.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin")
public class AdminOrderController {

    private final OrderService service;

    public AdminOrderController(OrderService service) {
        this.service = service;
    }

    @Operation(summary = "Daftar semua order, filter status (ADMIN)")
    @GetMapping
    public Page<OrderResponse> list(@RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listAll(status, pageable);
    }

    @Operation(summary = "Detail order apa pun (ADMIN)")
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return service.getAny(id);
    }

    @Operation(summary = "Majukan status order: PROCESSING, SHIPPED, COMPLETED (ADMIN)")
    @PatchMapping("/{id}/status")
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return service.adminChangeStatus(id, request.status());
    }
}
