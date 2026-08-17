package com.dinepilot.billing.client;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class OrderClient {
    private final RestClient client;
    private final SecretKey signingKey;

    public OrderClient(RestClient.Builder builder, @Value("${jwt.secret}") String jwtSecret) {
        this.client = builder.baseUrl("http://ORDER-SERVICE").build();
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public OrderSnapshot getOrder(String orderId) {
        try {
            ApiResponse<OrderSnapshot> response = client.get()
                    .uri("/api/orders/internal/{id}", orderId)
                    .header("Authorization", "Bearer " + buildInternalToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || response.data() == null) throw new ResourceNotFoundException("Order not found");
            return response.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new ResourceNotFoundException("Order not found");
            throw new ConflictException("Order service could not validate the order");
        }
    }

    private String buildInternalToken() {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject("billing-service")
                .claim("role", "SUPER_ADMIN")
                .issuedAt(new Date(now))
                .expiration(new Date(now + 300_000))
                .signWith(signingKey)
                .compact();
    }

    public record OrderSnapshot(String id, String userId, String restaurantId, String status, BigDecimal total) { }
}
