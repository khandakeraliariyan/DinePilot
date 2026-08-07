package com.dinepilot.restaurant.dto;

import com.dinepilot.restaurant.enums.TableStatus;

public record TableResponse(
        String id,
        String restaurantId,
        String tableNumber,
        int capacity,
        TableStatus status
) {
}
