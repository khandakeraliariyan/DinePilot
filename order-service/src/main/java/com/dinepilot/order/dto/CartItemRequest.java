package com.dinepilot.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CartItemRequest(@NotBlank String foodId, @Min(1) int quantity) {
}
