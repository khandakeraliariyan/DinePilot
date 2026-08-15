package com.dinepilot.common.event;

import java.time.Instant;

public record ReservationStatusChangedEvent(
        String eventId,
        Instant occurredAt,
        String reservationId,
        String restaurantId,
        String tableId,
        String status
) { }
