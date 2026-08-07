package com.dinepilot.restaurant.dto;

import com.dinepilot.restaurant.enums.TableStatus;
import jakarta.validation.constraints.NotNull;

public record TableStatusUpdateRequest(
        @NotNull TableStatus status
) {
}
