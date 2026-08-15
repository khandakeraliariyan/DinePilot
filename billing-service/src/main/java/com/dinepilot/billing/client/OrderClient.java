package com.dinepilot.billing.client;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Component
public class OrderClient {
    private final RestClient client;

    public OrderClient(RestClient.Builder builder) {
        this.client = builder.baseUrl("http://ORDER-SERVICE").build();
    }

    public OrderSnapshot getOrder(String orderId) {
        try {
            ApiResponse<OrderSnapshot> response = client.get().uri("/api/orders/{id}", orderId).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || response.data() == null) throw new ResourceNotFoundException("Order not found");
            return response.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new ResourceNotFoundException("Order not found");
            throw new ConflictException("Order service could not validate the order");
        }
    }

    public record OrderSnapshot(String id, String userId, String restaurantId, String status, BigDecimal total) { }
}
