package com.tokoku.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.tokoku.order.OrderDtos.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService service;

    public AdminOrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public Page<OrderResponse> list(@RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listAll(status, pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return service.getAny(id);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return service.adminChangeStatus(id, request.status());
    }
}
