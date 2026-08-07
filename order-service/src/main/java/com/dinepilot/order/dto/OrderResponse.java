package com.dinepilot.order.dto;

import com.dinepilot.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(String id, String userId, String restaurantId, OrderStatus status,
                            List<Item> items, BigDecimal total, Instant createdAt, Instant updatedAt) {
    public record Item(String foodId, String foodName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) { }
}
