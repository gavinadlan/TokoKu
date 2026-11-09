package com.tokoku.cart;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Cart() {}

    public Cart(Long userId) { this.userId = userId; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
}
