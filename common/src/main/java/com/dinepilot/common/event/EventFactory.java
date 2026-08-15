package com.dinepilot.common.event;

import java.time.Instant;
import java.util.UUID;

public final class EventFactory {
    private EventFactory() { }

    public static String eventId() {
        return UUID.randomUUID().toString();
    }

    public static Instant now() {
        return Instant.now();
    }
}
