package com.dinepilot.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TableRequest(
        @NotBlank String restaurantId,
        @NotBlank String tableNumber,
        @Positive int capacity
) {
}
