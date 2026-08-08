package com.dinepilot.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ReservationRequest(
        @NotBlank String tableId,
        @Positive int partySize,
        @NotNull @Future Instant reservedFor,
        @Size(max = 500) String notes
) {
}
