package com.hasnain.paymentworker.messaging;

public record PaymentProcessedEvent(Long orderId, boolean paymentSucceeded, String failureReason) {}
