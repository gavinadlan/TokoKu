package com.tokoku.order;

import java.util.EnumSet;
import java.util.Set;

// State machine eksplisit: setiap status tahu ke status mana boleh pindah
public enum OrderStatus {
    PENDING_PAYMENT, PAID, PROCESSING, SHIPPED, COMPLETED, CANCELLED;

    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(PROCESSING);
            case PROCESSING -> EnumSet.of(SHIPPED);
            case SHIPPED -> EnumSet.of(COMPLETED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext().contains(next);
    }
}
