package com.example.orderflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank String customerId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency) {
}
