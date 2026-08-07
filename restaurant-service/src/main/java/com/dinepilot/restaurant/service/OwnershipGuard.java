package com.dinepilot.restaurant.service;

import com.dinepilot.common.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class OwnershipGuard {

    public void checkOwnerOrSuperAdmin(Authentication authentication, String ownerId) {
        boolean isSuperAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));

        if (isSuperAdmin) {
            return;
        }

        if (!authentication.getName().equals(ownerId)) {
            throw new ForbiddenException("You do not own this resource");
        }
    }
}
