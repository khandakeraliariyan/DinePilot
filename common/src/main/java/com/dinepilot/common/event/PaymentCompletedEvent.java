package com.dinepilot.common.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCompletedEvent(
                String eventId,
                Instant occurredAt,
                String paymentId,
                String invoiceId,
                String orderId,
                String userId,
                String restaurantId,
                BigDecimal amount) implements java.io.Serializable {
}
