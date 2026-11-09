package com.tokoku.cart;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.tokoku.cart.CartDtos.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal Jwt jwt) {
        return service.get(Long.valueOf(jwt.getSubject()));
    }

    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddItemRequest request) {
        return service.add(Long.valueOf(jwt.getSubject()), request);
    }

    @PutMapping("/items/{id}")
    public CartResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody UpdateItemRequest request) {
        return service.update(Long.valueOf(jwt.getSubject()), id, request);
    }

    @DeleteMapping("/items/{id}")
    public CartResponse remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.remove(Long.valueOf(jwt.getSubject()), id);
    }
}
