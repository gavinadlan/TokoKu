package com.tokoku.order;

import java.util.EnumSet;
import java.util.Set;

// State machine eksplisit per jalur: kirim (DELIVERY) dan ambil di toko (PICKUP)
public enum OrderStatus {
    PENDING_PAYMENT, PAID, PROCESSING, SHIPPED, READY_FOR_PICKUP, COMPLETED, CANCELLED;

    public Set<OrderStatus> allowedNext(FulfillmentType type) {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(PROCESSING);
            case PROCESSING -> EnumSet.of(type == FulfillmentType.DELIVERY ? SHIPPED : READY_FOR_PICKUP);
            case SHIPPED, READY_FOR_PICKUP -> EnumSet.of(COMPLETED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus next, FulfillmentType type) {
        return allowedNext(type).contains(next);
    }
}
