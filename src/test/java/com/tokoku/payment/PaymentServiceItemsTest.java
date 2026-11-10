package com.tokoku.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tokoku.auth.Role;
import com.tokoku.auth.User;
import com.tokoku.auth.UserRepository;
import com.tokoku.order.FulfillmentType;
import com.tokoku.order.Order;
import com.tokoku.order.OrderRepository;
import com.tokoku.order.OrderService;
import com.tokoku.payment.PaymentGateway.PaymentRequest;
import com.tokoku.payment.PaymentGateway.PaymentSession;

/** Jumlah item_details (termasuk ongkir) harus sama dengan gross_amount yang dikirim ke provider. */
class PaymentServiceItemsTest {

    @Test
    void shippingIsSentAsOwnItemAndItemsSumToAmount() {
        PaymentRepository payments = mock(PaymentRepository.class);
        OrderRepository orders = mock(OrderRepository.class);
        UserRepository users = mock(UserRepository.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        PaymentService service = new PaymentService(payments, orders, mock(OrderService.class), users, gateway, 60);

        Order order = new Order(7L, FulfillmentType.DELIVERY, "Budi", "0812", "Jl. Kopi No. 12, Jakarta");
        order.addItem(1L, "Kopi Arabika Gayo 250g", 2, new BigDecimal("85000"));
        order.addItem(2L, "Dripper V60", 1, new BigDecimal("165000"));
        order.applyShippingFee(new BigDecimal("15000"));
        when(orders.findOwnedForUpdate(any(), any())).thenReturn(Optional.of(order));
        when(payments.findFirstByOrderIdAndStatus(any(), any())).thenReturn(Optional.empty());
        when(users.findById(any())).thenReturn(Optional.of(new User("b@example.com", "x", "Budi", Role.USER)));
        when(gateway.createPayment(any())).thenReturn(new PaymentSession("tok", "https://x"));
        when(payments.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        service.pay(7L, 1L);

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(gateway).createPayment(captor.capture());
        PaymentRequest r = captor.getValue();
        assertThat(r.amount()).isEqualByComparingTo("350000");
        assertThat(r.items()).extracting(PaymentGateway.Item::id).contains("SHIPPING");
        BigDecimal sum = r.items().stream().map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(r.amount());
    }

    @Test
    void noShippingItemWhenFeeIsZero() {
        PaymentRepository payments = mock(PaymentRepository.class);
        OrderRepository orders = mock(OrderRepository.class);
        UserRepository users = mock(UserRepository.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        PaymentService service = new PaymentService(payments, orders, mock(OrderService.class), users, gateway, 60);

        Order order = new Order(7L, FulfillmentType.PICKUP, "Budi", "0812", null);
        order.addItem(1L, "Kopi", 1, new BigDecimal("50000"));
        order.applyShippingFee(BigDecimal.ZERO);
        when(orders.findOwnedForUpdate(any(), any())).thenReturn(Optional.of(order));
        when(payments.findFirstByOrderIdAndStatus(any(), any())).thenReturn(Optional.empty());
        when(users.findById(any())).thenReturn(Optional.of(new User("b@example.com", "x", "Budi", Role.USER)));
        when(gateway.createPayment(any())).thenReturn(new PaymentSession("tok", "https://x"));
        when(payments.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        service.pay(7L, 1L);

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(gateway).createPayment(captor.capture());
        assertThat(captor.getValue().items()).extracting(PaymentGateway.Item::id).doesNotContain("SHIPPING");
    }
}
