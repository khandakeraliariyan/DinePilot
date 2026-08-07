package com.dinepilot.restaurant.dto;

import java.math.BigDecimal;

public record FoodResponse(
        String id,
        String restaurantId,
        String categoryId,
        String name,
        String description,
        BigDecimal price,
        boolean available
) {
}
