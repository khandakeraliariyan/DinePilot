package com.dinepilot.reservation.service;

import com.dinepilot.common.exception.ForbiddenException;
import com.dinepilot.reservation.client.RestaurantServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantAccessGuardTest {

    private static final String RESTAURANT_ID = "restaurant-1";
    private static final String OWNER_ID = "owner-1";

    @Mock
    private RestaurantServiceClient restaurantClient;

    @Mock
    private Authentication authentication;

    private RestaurantAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new RestaurantAccessGuard(restaurantClient);
    }

    @Test
    void allowsASuperAdminWithoutCallingRestaurantService() {
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        guard.checkManagesRestaurant(authentication, RESTAURANT_ID);

        verify(restaurantClient, never()).getRestaurant(any());
    }

    @Test
    void allowsTheOwningRestaurantAdmin() {
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> List.of(new SimpleGrantedAuthority("ROLE_RESTAURANT_ADMIN")));
        when(authentication.getName()).thenReturn(OWNER_ID);
        when(restaurantClient.getRestaurant(RESTAURANT_ID))
                .thenReturn(new RestaurantServiceClient.RestaurantInfo(RESTAURANT_ID, OWNER_ID, "Bistro"));

        assertThatNoException().isThrownBy(() -> guard.checkManagesRestaurant(authentication, RESTAURANT_ID));
    }

    @Test
    void rejectsANonOwningRestaurantAdmin() {
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> List.of(new SimpleGrantedAuthority("ROLE_RESTAURANT_ADMIN")));
        when(authentication.getName()).thenReturn("someone-else");
        when(restaurantClient.getRestaurant(RESTAURANT_ID))
                .thenReturn(new RestaurantServiceClient.RestaurantInfo(RESTAURANT_ID, OWNER_ID, "Bistro"));

        assertThatThrownBy(() -> guard.checkManagesRestaurant(authentication, RESTAURANT_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You do not manage this restaurant");
    }
}
