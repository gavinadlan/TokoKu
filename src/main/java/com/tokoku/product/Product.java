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
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    @org.hibernate.annotations.BatchSize(size = 50)
    private java.util.List<ProductImage> images = new java.util.ArrayList<>();

    @Version
    private Long version;

    protected Product() {}

    public Product(Category category, String name, String description, BigDecimal price, int stock) {
        update(category, name, description, price);
        setStock(stock);
    }

    public void update(Category category, String name, String description, BigDecimal price) {
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
    public java.util.List<ProductImage> getImages() { return images; }
}
