package com.hasnain.orderapi.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record CreateOrderRequest(
    @NotEmpty(message = "Order must contain at least one item")
    Map<Long, Integer> items
) {}
