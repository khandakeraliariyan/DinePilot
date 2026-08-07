package com.dinepilot.user.dto;

public record AddressResponse(
        String id,
        String label,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean defaultAddress
) {
}
