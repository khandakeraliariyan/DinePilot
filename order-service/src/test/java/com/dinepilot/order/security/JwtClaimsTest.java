package com.dinepilot.order.security;

import com.dinepilot.common.exception.ForbiddenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtClaimsTest {

    @Test
    void readsRestaurantScopeFromAuthenticationDetails() {
        Claims claims = Jwts.claims().add(Map.of("restaurantId", "restaurant-1")).build();
        var authentication = new UsernamePasswordAuthenticationToken("kitchen-1", null);
        authentication.setDetails(claims);

        assertThat(JwtClaims.restaurantId(authentication)).isEqualTo("restaurant-1");
    }

    @Test
    void rejectsAuthenticationWithoutParsedClaims() {
        var authentication = new UsernamePasswordAuthenticationToken("kitchen-1", null);

        assertThatThrownBy(() -> JwtClaims.restaurantId(authentication))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Kitchen user is not assigned to a restaurant");
    }

    @Test
    void rejectsClaimsWithoutRestaurantScope() {
        Claims claims = Jwts.claims().add(Map.of("role", "KITCHEN")).build();
        var authentication = new UsernamePasswordAuthenticationToken("kitchen-1", null);
        authentication.setDetails(claims);

        assertThatThrownBy(() -> JwtClaims.restaurantId(authentication))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Kitchen user is not assigned to a restaurant");
    }

    @Test
    void rejectsABlankRestaurantScope() {
        Claims claims = Jwts.claims().add(Map.of("restaurantId", "   ")).build();
        var authentication = new UsernamePasswordAuthenticationToken("kitchen-1", null);
        authentication.setDetails(claims);

        assertThatThrownBy(() -> JwtClaims.restaurantId(authentication))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Kitchen user is not assigned to a restaurant");
    }
}
