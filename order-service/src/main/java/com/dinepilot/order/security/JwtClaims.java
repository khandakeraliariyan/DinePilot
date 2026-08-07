package com.dinepilot.order.security;

import com.dinepilot.common.exception.ForbiddenException;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;

public final class JwtClaims {
    private JwtClaims() { }

    public static String restaurantId(Authentication authentication) {
        if (authentication.getDetails() instanceof Claims claims) {
            String restaurantId = claims.get("restaurantId", String.class);
            if (restaurantId != null && !restaurantId.isBlank()) return restaurantId;
        }
        throw new ForbiddenException("Kitchen user is not assigned to a restaurant");
    }
}
