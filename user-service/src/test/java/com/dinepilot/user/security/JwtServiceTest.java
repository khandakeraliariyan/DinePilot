package com.dinepilot.user.security;

import com.dinepilot.common.enums.Role;
import com.dinepilot.common.security.JwtValidator;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET =
            "test-secret-that-is-long-enough-for-hmac-sha-signatures-1234567890";

    private JwtService service;
    private JwtValidator validator;

    @BeforeEach
    void setUp() {
        service = new JwtService(SECRET, 900_000);
        validator = new JwtValidator(SECRET);
    }

    @Test
    void includesIdentityAndRoleClaims() {
        String token = service.generateAccessToken(
                "user-1",
                "customer@example.com",
                Role.CUSTOMER,
                null
        );

        Claims claims = validator.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("email", String.class)).isEqualTo("customer@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
        assertThat(claims.get("restaurantId")).isNull();
    }

    @Test
    void includesRestaurantScopeForAnAssignedKitchenUser() {
        String token = service.generateAccessToken(
                "kitchen-1",
                "cook@example.com",
                Role.KITCHEN,
                "restaurant-1"
        );

        Claims claims = validator.parseAndValidate(token);

        assertThat(claims.get("role", String.class)).isEqualTo("KITCHEN");
        assertThat(claims.get("restaurantId", String.class)).isEqualTo("restaurant-1");
    }

    @Test
    void omitsRestaurantScopeWhenTheValueIsBlank() {
        String token = service.generateAccessToken(
                "kitchen-1",
                "cook@example.com",
                Role.KITCHEN,
                "   "
        );

        Claims claims = validator.parseAndValidate(token);

        assertThat(claims.get("restaurantId")).isNull();
    }

    @Test
    void reportsTheConfiguredAccessTokenLifetime() {
        assertThat(service.getAccessTokenExpirationMs()).isEqualTo(900_000);
    }

    @Test
    void createsAnExpirationAfterTheIssueTime() {
        String token = service.generateAccessToken(
                "user-1",
                "customer@example.com",
                Role.CUSTOMER,
                null
        );

        Claims claims = validator.parseAndValidate(token);

        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(900_000);
    }
}
