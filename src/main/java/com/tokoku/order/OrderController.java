package com.tokoku.order;

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
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest request) {
        return service.checkout(Long.valueOf(jwt.getSubject()), request);
    }

    @GetMapping
    public Page<OrderResponse> list(@AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listMine(Long.valueOf(jwt.getSubject()), pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getMine(Long.valueOf(jwt.getSubject()), id);
    }
}
