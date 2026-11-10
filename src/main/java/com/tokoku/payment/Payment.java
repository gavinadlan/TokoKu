package com.tokoku.payment;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(name = "provider_ref", nullable = false, unique = true, updatable = false)
    private String providerRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    private String token;

    @Column(name = "redirect_url")
    private String redirectUrl;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Payment() {}

    public Payment(Long orderId, String providerRef, BigDecimal amount, String token, String redirectUrl) {
        this.orderId = orderId;
        this.providerRef = providerRef;
        this.amount = amount;
        this.token = token;
        this.redirectUrl = redirectUrl;
    }

    public void markPaid(String paymentType) {
        status = PaymentStatus.PAID;
        paidAt = Instant.now();
        this.paymentType = paymentType;
    }

    public void markEnded(PaymentStatus ended) { status = ended; }

    public boolean isPending() { return status == PaymentStatus.PENDING; }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getProviderRef() { return providerRef; }
    public PaymentStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getToken() { return token; }
    public String getRedirectUrl() { return redirectUrl; }
    public Instant getPaidAt() { return paidAt; }
    public String getPaymentType() { return paymentType; }
}
