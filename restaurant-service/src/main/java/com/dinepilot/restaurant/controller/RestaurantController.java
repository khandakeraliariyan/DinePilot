package com.dinepilot.restaurant.controller;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.restaurant.dto.RestaurantRequest;
import com.dinepilot.restaurant.dto.RestaurantResponse;
import com.dinepilot.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('RESTAURANT_ADMIN')")
    public ApiResponse<RestaurantResponse> create(
            Authentication authentication,
            @Valid @RequestBody RestaurantRequest request
    ) {
        return ApiResponse.success("Restaurant created", restaurantService.create(authentication, request));
    }

    @GetMapping
    public ApiResponse<List<RestaurantResponse>> list() {
        return ApiResponse.success(restaurantService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<RestaurantResponse> get(@PathVariable String id) {
        return ApiResponse.success(restaurantService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<RestaurantResponse> update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody RestaurantRequest request
    ) {
        return ApiResponse.success("Restaurant updated", restaurantService.update(authentication, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable String id) {
        restaurantService.delete(authentication, id);
        return ApiResponse.success("Restaurant deleted", null);
    }
}
