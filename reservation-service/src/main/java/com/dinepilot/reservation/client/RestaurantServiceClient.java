package com.dinepilot.reservation.client;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestaurantServiceClient {
    private final RestClient client;

    public RestaurantServiceClient(RestClient.Builder builder) {
        this.client = builder.baseUrl("http://RESTAURANT-SERVICE").build();
    }

    public TableInfo getTable(String tableId) {
        try {
            ApiResponse<TableInfo> response = client.get().uri("/api/tables/{id}", tableId).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || response.data() == null) throw new ResourceNotFoundException("Table not found");
            return response.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new ResourceNotFoundException("Table not found");
            throw new ConflictException("Restaurant service could not validate the table");
        }
    }

    public RestaurantInfo getRestaurant(String restaurantId) {
        try {
            ApiResponse<RestaurantInfo> response = client.get().uri("/api/restaurants/{id}", restaurantId).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || response.data() == null) throw new ResourceNotFoundException("Restaurant not found");
            return response.data();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new ResourceNotFoundException("Restaurant not found");
            throw new ConflictException("Restaurant service could not validate the restaurant");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TableInfo(String id, String restaurantId, String tableNumber, int capacity) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RestaurantInfo(String id, String ownerId, String name) { }
}
