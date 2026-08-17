package com.dinepilot.common.event;

import java.time.Instant;

public record OrderStatusChangedEvent(
                String eventId,
                Instant occurredAt,
                String orderId,
                String userId,
                String restaurantId,
                String status) implements java.io.Serializable {
}
