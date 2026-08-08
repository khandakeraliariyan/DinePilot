package com.dinepilot.reservation.dto;

import com.dinepilot.reservation.enums.ReservationStatus;

import java.time.Instant;

public record ReservationResponse(
        String id,
        String userId,
        String restaurantId,
        String tableId,
        int partySize,
        Instant reservedFor,
        ReservationStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
