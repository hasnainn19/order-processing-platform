package com.hasnain.orderapi.messaging;

public record PaymentProcessedEvent(Long orderId, boolean paymentSucceeded, String failureReason) {}
