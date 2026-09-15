package com.hasnain.orderapi.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateProductRequest(
    @NotBlank(message = "Name is required")
    String name,
    @Positive(message = "Price must be greater than zero")
    BigDecimal price,
    @PositiveOrZero(message = "Stock quantity cannot be negative")
    int stockQuantity
) {}
