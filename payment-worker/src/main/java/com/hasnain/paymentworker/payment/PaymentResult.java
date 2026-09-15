package com.hasnain.paymentworker.payment;

public record PaymentResult(boolean succeeded, String failureReason) {

    public static PaymentResult success() {
        return new PaymentResult(true, null);
    }

    public static PaymentResult declined(String reason) {
        return new PaymentResult(false, reason);
    }
}
