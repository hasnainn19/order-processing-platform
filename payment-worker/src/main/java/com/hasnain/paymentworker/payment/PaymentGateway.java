package com.hasnain.paymentworker.payment;

public interface PaymentGateway {

    PaymentResult charge(Long orderId);
}
