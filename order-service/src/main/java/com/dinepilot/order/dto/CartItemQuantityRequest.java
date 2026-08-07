package com.dinepilot.order.dto;

import jakarta.validation.constraints.Min;

public record CartItemQuantityRequest(@Min(1) int quantity) {
}
