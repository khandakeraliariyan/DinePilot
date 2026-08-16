package com.dinepilot.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
                String eventId,
                Instant occurredAt,
                String orderId,
                String userId,
                String restaurantId,
                BigDecimal total) implements java.io.Serializable {
}
