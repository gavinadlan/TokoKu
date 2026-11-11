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

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_type", nullable = false, updatable = false)
    private FulfillmentType fulfillmentType = FulfillmentType.DELIVERY;

    @Column(name = "shipping_fee", nullable = false)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    private String courier;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "pickup_code")
    private String pickupCode;

    @Column(name = "pickup_ready_at")
    private Instant pickupReadyAt;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "invoiced_at")
    private Instant invoicedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @BatchSize(size = 50) // hindari N+1 saat memuat banyak order sekaligus
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    public Order(Long userId, FulfillmentType fulfillmentType, String shippingName, String shippingPhone, String shippingAddress) {
        this.userId = userId;
        this.fulfillmentType = fulfillmentType;
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
    }

    public void addItem(Long productId, String productName, int quantity, BigDecimal unitPrice) {
        items.add(new OrderItem(this, productId, productName, quantity, unitPrice));
        totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    /** Ongkir dihitung server setelah semua item masuk; total = subtotal + ongkir. */
    public void applyShippingFee(BigDecimal fee) {
        shippingFee = fee;
        totalAmount = totalAmount.add(fee);
    }

    public BigDecimal getSubtotal() { return totalAmount.subtract(shippingFee); }

    public void markShipped(String courier, String trackingNumber) {
        transitionTo(OrderStatus.SHIPPED);
        this.courier = courier;
        this.trackingNumber = trackingNumber;
    }

    public void markReadyForPickup() {
        transitionTo(OrderStatus.READY_FOR_PICKUP);
        this.pickupReadyAt = Instant.now();
    }

    public void assignPickupCode(String code) { this.pickupCode = code; }

    /** Idempoten: nomor invoice hanya diberikan sekali. */
    public void assignInvoice(String number, Instant at) {
        if (invoiceNumber == null) {
            invoiceNumber = number;
            invoicedAt = at;
        }
    }

    public boolean pickupCodeMatches(String input) {
        return pickupCode != null && pickupCode.equals(PickupCodes.normalize(input));
    }

    // Satu-satunya pintu mengubah status; transisi tidak sah ditolak (409)
    public void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next, fulfillmentType)) {
            throw new ConflictException("Cannot change order status from " + status + " to " + next);
        }
        status = next;
    }

    public boolean isOverdue(Instant cutoff) {
        return status == OrderStatus.PENDING_PAYMENT && createdAt.isBefore(cutoff);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getShippingName() { return shippingName; }
    public String getShippingPhone() { return shippingPhone; }
    public String getShippingAddress() { return shippingAddress; }
    public FulfillmentType getFulfillmentType() { return fulfillmentType; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public String getCourier() { return courier; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getPickupCode() { return pickupCode; }
    public Instant getPickupReadyAt() { return pickupReadyAt; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public Instant getInvoicedAt() { return invoicedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
}
