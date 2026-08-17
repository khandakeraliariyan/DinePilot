package com.dinepilot.common.event;

import java.time.Instant;

public record ReservationCreatedEvent(
                String eventId,
                Instant occurredAt,
                String reservationId,
                String userId,
                String restaurantId,
                String tableId,
                Instant reservedFor) implements java.io.Serializable {
}
