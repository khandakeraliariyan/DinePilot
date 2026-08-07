package com.dinepilot.restaurant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RestaurantRequest(
        @NotBlank String name,

        String description,

        @NotNull @Valid AddressDto address,

        @Valid List<OpeningHoursDto> openingHours,

        boolean active
) {
}
