package com.tokoku.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.tokoku.common.ConflictException;

class OrderStatusTest {

    @Test
    void legalTransitions() {
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.PAID)).isTrue();
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.PROCESSING)).isTrue();
        assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
        assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.COMPLETED)).isTrue();
    }

    @Test
    void everythingElseIsRejected() {
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.COMPLETED.allowedNext()).isEmpty();
        assertThat(OrderStatus.CANCELLED.allowedNext()).isEmpty();
    }

    @Test
    void orderRejectsIllegalTransitionWith409Exception() {
        Order o = new Order(1L, "a", "b", "c");
        assertThatThrownBy(() -> o.transitionTo(OrderStatus.SHIPPED)).isInstanceOf(ConflictException.class);
        o.transitionTo(OrderStatus.PAID);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
