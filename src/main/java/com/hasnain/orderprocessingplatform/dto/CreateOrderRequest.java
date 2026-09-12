package com.hasnain.orderprocessingplatform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record CreateOrderRequest(

    @NotNull(message = "User ID is required")
    Long userId,

    @NotEmpty(message = "Order must contain at least one item")
    Map<Long, Integer> items
) {}
