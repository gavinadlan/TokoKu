package com.tokoku.product;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "public_id")
    private String publicId;

    @Column(nullable = false)
    private String url;

    private Integer width;
    private Integer height;

    @Column(nullable = false)
    private int position;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ProductImage() {}

    public ProductImage(Product product, String publicId, String url, Integer width, Integer height, int position, boolean primary) {
        this.product = product;
        this.publicId = publicId;
        this.url = url;
        this.width = width;
        this.height = height;
        this.position = position;
        this.primary = primary;
    }

    public void setPosition(int position) { this.position = position; }
    public void setPrimary(boolean primary) { this.primary = primary; }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public String getUrl() { return url; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public int getPosition() { return position; }
    public boolean isPrimary() { return primary; }
}
