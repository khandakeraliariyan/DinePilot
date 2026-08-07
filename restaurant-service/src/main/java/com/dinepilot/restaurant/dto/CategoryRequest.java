package com.dinepilot.restaurant.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String restaurantId,
        @NotBlank String name,
        String description,
        int displayOrder
) {
}
