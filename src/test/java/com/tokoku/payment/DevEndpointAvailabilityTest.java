package com.tokoku.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

import com.tokoku.order.OrderRepository;

// Bukti: endpoint simulasi tidak terdaftar di prod, dan fake gateway menolak start di prod
class DevEndpointAvailabilityTest {

    private ApplicationContextRunner runner(String profile) {
        return new ApplicationContextRunner()
                .withInitializer(ctx -> { if (profile != null) ctx.getEnvironment().setActiveProfiles(profile); })
                .withPropertyValues("app.payment.provider=fake")
                .withBean(PaymentService.class, () -> mock(PaymentService.class))
                .withBean(PaymentRepository.class, () -> mock(PaymentRepository.class))
                .withBean(OrderRepository.class, () -> mock(OrderRepository.class))
                .withBean(FakePaymentGateway.class, () -> new FakePaymentGateway(new MockEnvironment()))
                .withUserConfiguration(DevPaymentController.class);
    }

    @Test
    void presentInDev() {
        runner(null).run(ctx -> assertThat(ctx).hasSingleBean(DevPaymentController.class));
    }

    @Test
    void absentInProd() {
        runner("prod").run(ctx -> assertThat(ctx).doesNotHaveBean(DevPaymentController.class));
    }

    @Test
    void absentWhenProviderIsMidtrans() {
        runner(null).withPropertyValues("app.payment.provider=midtrans")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(DevPaymentController.class));
    }

    @Test
    void fakeGatewayRefusesToStartInProd() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new FakePaymentGateway(prod))
                .isInstanceOf(IllegalStateException.class);
    }
}
