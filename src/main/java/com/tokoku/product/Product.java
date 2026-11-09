package com.tokoku.product;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean active = true;

    // Optimistic locking: Hibernate menaikkan version tiap update. Jika dua request mengubah
    // baris yang sama bersamaan, yang kedua gagal (409) sehingga stok tidak saling menimpa.
    @Column(name = "image_url")
    private String imageUrl;

    @Version
    private Long version;

    protected Product() {}

    public Product(Category category, String name, String description, BigDecimal price, int stock) {
        update(category, name, description, price, null);
        setStock(stock);
    }

    public void update(Category category, String name, String description, BigDecimal price, String imageUrl) {
        this.imageUrl = imageUrl;
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public void setStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("Stock must not be negative");
        }
        this.stock = stock;
    }

    public void deactivate() { this.active = false; }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isActive() { return active; }
    public String getImageUrl() { return imageUrl; }
}
