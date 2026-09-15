package com.hasnain.orderapi.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long productId,
    String productName,
    int quantity,
    BigDecimal price
) {}
