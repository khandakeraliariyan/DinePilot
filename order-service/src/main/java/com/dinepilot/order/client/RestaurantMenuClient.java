package com.dinepilot.order.client;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.math.BigDecimal;

@Component
public class RestaurantMenuClient {
    private final RestClient client;

    public RestaurantMenuClient(RestClient.Builder builder) {
        this.client = builder.baseUrl("http://RESTAURANT-SERVICE").build();
    }

    public MenuItem getAvailableFood(String foodId) {
        try {
            ApiResponse<MenuItem> response = client.get().uri("/api/menu/{id}", foodId).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || response.data() == null) throw new ResourceNotFoundException("Food not found");
            if (!response.data().available()) throw new ConflictException("Food is currently unavailable");
            return response.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new ResourceNotFoundException("Food not found");
            throw new ConflictException("Restaurant service could not validate the food item");
        }
    }

    public record MenuItem(String id, String restaurantId, String categoryId, String name,
                           String description, BigDecimal price, boolean available) { }
}
