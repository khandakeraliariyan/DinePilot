package com.dinepilot.user.dto;

import com.dinepilot.common.enums.Role;

public record UserProfileResponse(
        String id,
        String email,
        String fullName,
        String phone,
        Role role
) {
}
