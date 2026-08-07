package com.dinepilot.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        String label,

        @NotBlank String line1,

        String line2,

        @NotBlank String city,

        String state,

        @NotBlank String postalCode,

        @NotBlank String country,

        boolean defaultAddress
) {
}
