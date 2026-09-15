package com.hasnain.paymentworker.payment;

import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Stands in for a real provider client (e.g. Stripe).
 * Splits outcomes into three cases:
 * 
 * 1. A definitive decline is a normal PaymentResult (never retried).
 * 2. An unreachable/erroring provider throws so the caller's retry+DLQ handling applies.
 * 3. A successful charge returns a normal PaymentResult.
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private final Random random;

    public SimulatedPaymentGateway() {
        this(new Random());
    }

    SimulatedPaymentGateway(Random random) {
        this.random = random;
    }

    @Override
    public PaymentResult charge(Long orderId) {
        int roll = random.nextInt(100);

        if (roll < 70) {
            return PaymentResult.success();
        }
        if (roll < 90) {
            return PaymentResult.declined("card_declined");
        }
        throw new PaymentProviderException("Payment provider unreachable for order: " + orderId);
    }
}
