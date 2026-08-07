package com.dinepilot.restaurant.dto;

public record CategoryResponse(
        String id,
        String restaurantId,
        String name,
        String description,
        int displayOrder
) {
}
