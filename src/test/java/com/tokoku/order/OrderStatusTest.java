package com.tokoku.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.tokoku.common.ConflictException;

class OrderStatusTest {

    private static final FulfillmentType D = FulfillmentType.DELIVERY;
    private static final FulfillmentType P = FulfillmentType.PICKUP;

    @Test
    void deliveryPath() {
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.PAID, D)).isTrue();
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.CANCELLED, D)).isTrue();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.PROCESSING, D)).isTrue();
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED, D)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.COMPLETED, D)).isTrue();
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.READY_FOR_PICKUP, D)).isFalse();
    }

    @Test
    void pickupPath() {
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.READY_FOR_PICKUP, P)).isTrue();
        assertThat(OrderStatus.READY_FOR_PICKUP.canTransitionTo(OrderStatus.COMPLETED, P)).isTrue();
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED, P)).isFalse();
        assertThat(OrderStatus.SHIPPED.allowedNext(P)).containsExactly(OrderStatus.COMPLETED); // tidak dicapai jalur ambil, tetapi aturan tabel tetap
    }

    @Test
    void everythingElseIsRejected() {
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.SHIPPED, D)).isFalse();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED, D)).isFalse();
        assertThat(OrderStatus.COMPLETED.allowedNext(D)).isEmpty();
        assertThat(OrderStatus.CANCELLED.allowedNext(P)).isEmpty();
    }

    @Test
    void orderRejectsIllegalTransitionWith409Exception() {
        Order o = new Order(1L, D, "a", "b", "c");
        assertThatThrownBy(() -> o.transitionTo(OrderStatus.SHIPPED)).isInstanceOf(ConflictException.class);
        o.transitionTo(OrderStatus.PAID);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
