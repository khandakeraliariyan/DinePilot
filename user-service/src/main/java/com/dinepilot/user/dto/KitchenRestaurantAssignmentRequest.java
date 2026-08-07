package com.dinepilot.user.dto;

import jakarta.validation.constraints.NotBlank;

public record KitchenRestaurantAssignmentRequest(@NotBlank String restaurantId) {
}
