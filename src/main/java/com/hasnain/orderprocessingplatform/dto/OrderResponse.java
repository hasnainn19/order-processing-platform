package com.hasnain.orderprocessingplatform.dto;

import com.hasnain.orderprocessingplatform.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    Long userId,
    String userEmail,
    OrderStatus status,
    BigDecimal total,
    LocalDateTime createdAt,
    List<OrderItemResponse> items
) {}
