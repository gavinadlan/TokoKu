package com.tokoku.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.tokoku.common.ConflictException;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "shipping_name", nullable = false)
    private String shippingName;

    @Column(name = "shipping_phone", nullable = false)
    private String shippingPhone;

    @Column(name = "shipping_address", nullable = false)
    private String shippingAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @BatchSize(size = 50) // hindari N+1 saat memuat banyak order sekaligus
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    public Order(Long userId, String shippingName, String shippingPhone, String shippingAddress) {
        this.userId = userId;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
    }

    public void addItem(Long productId, String productName, int quantity, BigDecimal unitPrice) {
        items.add(new OrderItem(this, productId, productName, quantity, unitPrice));
        totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    // Satu-satunya pintu mengubah status; transisi tidak sah ditolak (409)
    public void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new ConflictException("Cannot change order status from " + status + " to " + next);
        }
        status = next;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getShippingName() { return shippingName; }
    public String getShippingPhone() { return shippingPhone; }
    public String getShippingAddress() { return shippingAddress; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
}
