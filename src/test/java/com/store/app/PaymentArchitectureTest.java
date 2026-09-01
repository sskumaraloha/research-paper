package com.store.app;

import com.store.app.exception.BusinessValidationException;
import com.store.app.order.entity.OrderStatus;
import com.store.app.payment.entity.PaymentMethod;
import com.store.app.payment.service.PaymentService;
import com.store.app.payment.service.PaymentServiceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the payment strategy registry: COD is routed to its
 * implementation, unintegrated methods are rejected clearly.
 */
@SpringBootTest
@ActiveProfiles("test")
class PaymentArchitectureTest {

    @Autowired
    private PaymentServiceRegistry registry;

    @Test
    void codIsSupportedAndConfirmsOrdersImmediately() {
        assertThat(registry.isSupported(PaymentMethod.CASH_ON_DELIVERY)).isTrue();

        PaymentService cod = registry.getService(PaymentMethod.CASH_ON_DELIVERY);
        assertThat(cod.getSupportedMethod()).isEqualTo(PaymentMethod.CASH_ON_DELIVERY);
        assertThat(cod.initialOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void onlineIsDefinedButNotIntegratedYet() {
        assertThat(registry.isSupported(PaymentMethod.ONLINE)).isFalse();
        assertThatThrownBy(() -> registry.getService(PaymentMethod.ONLINE))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("not available yet");
    }
}
