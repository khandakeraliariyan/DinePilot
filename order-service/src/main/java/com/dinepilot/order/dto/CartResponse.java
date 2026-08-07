package com.dinepilot.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(String id, String restaurantId, List<Item> items, BigDecimal total) {
    public record Item(String foodId, String restaurantId, String foodName, BigDecimal unitPrice,
                       int quantity, BigDecimal lineTotal) { }
}
