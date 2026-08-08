package com.dinepilot.reservation.service;

import com.dinepilot.common.exception.ForbiddenException;
import com.dinepilot.reservation.client.RestaurantServiceClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class RestaurantAccessGuard {

    private final RestaurantServiceClient restaurantClient;

    public RestaurantAccessGuard(RestaurantServiceClient restaurantClient) {
        this.restaurantClient = restaurantClient;
    }

    public void checkManagesRestaurant(Authentication authentication, String restaurantId) {
        boolean isSuperAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        if (isSuperAdmin) return;

        RestaurantServiceClient.RestaurantInfo restaurant = restaurantClient.getRestaurant(restaurantId);
        if (!authentication.getName().equals(restaurant.ownerId())) {
            throw new ForbiddenException("You do not manage this restaurant");
        }
    }
}
