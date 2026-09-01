package com.store.app.payment.service;

import com.store.app.exception.BusinessValidationException;
import com.store.app.payment.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Routes payment operations to the {@link PaymentService} implementation
 * for a given method. Implementations register themselves simply by
 * being Spring beans; methods without an implementation (e.g. ONLINE
 * until a gateway is integrated) are rejected with a clear message.
 */
@Component
public class PaymentServiceRegistry {

    private final Map<PaymentMethod, PaymentService> services;

    public PaymentServiceRegistry(List<PaymentService> paymentServices) {
        Map<PaymentMethod, PaymentService> byMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentService service : paymentServices) {
            PaymentService previous = byMethod.put(service.getSupportedMethod(), service);
            if (previous != null) {
                throw new IllegalStateException("Two PaymentService beans claim method "
                        + service.getSupportedMethod() + ": "
                        + previous.getClass().getSimpleName() + " and "
                        + service.getClass().getSimpleName());
            }
        }
        this.services = byMethod;
    }

    /**
     * @throws BusinessValidationException if no implementation supports
     *         the method (it is defined but not yet integrated)
     */
    public PaymentService getService(PaymentMethod method) {
        PaymentService service = services.get(method);
        if (service == null) {
            throw new BusinessValidationException(
                    "Payment method " + method + " is not available yet. "
                            + "Please choose a different payment method.");
        }
        return service;
    }

    public boolean isSupported(PaymentMethod method) {
        return services.containsKey(method);
    }
}
