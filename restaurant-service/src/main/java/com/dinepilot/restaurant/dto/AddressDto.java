package com.dinepilot.restaurant.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressDto(
        @NotBlank String line1,
        @NotBlank String city,
        String state,
        @NotBlank String postalCode,
        @NotBlank String country
) {
}
