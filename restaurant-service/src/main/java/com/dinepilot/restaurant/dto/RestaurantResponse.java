package com.dinepilot.restaurant.dto;

import java.util.List;

public record RestaurantResponse(
        String id,
        String ownerId,
        String name,
        String description,
        AddressDto address,
        List<OpeningHoursDto> openingHours,
        boolean active
) {
}
