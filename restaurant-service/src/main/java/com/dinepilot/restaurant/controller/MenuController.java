package com.dinepilot.restaurant.controller;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.restaurant.dto.FoodAvailabilityRequest;
import com.dinepilot.restaurant.dto.FoodRequest;
import com.dinepilot.restaurant.dto.FoodResponse;
import com.dinepilot.restaurant.service.FoodService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final FoodService foodService;

    public MenuController(FoodService foodService) {
        this.foodService = foodService;
    }

    @PostMapping
    public ApiResponse<FoodResponse> create(
            Authentication authentication,
            @Valid @RequestBody FoodRequest request
    ) {
        return ApiResponse.success("Food created", foodService.create(authentication, request));
    }

    @GetMapping
    public ApiResponse<List<FoodResponse>> search(
            @RequestParam(required = false) String restaurantId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String q
    ) {
        return ApiResponse.success(foodService.search(restaurantId, categoryId, available, q));
    }

    @GetMapping("/{id}")
    public ApiResponse<FoodResponse> get(@PathVariable String id) {
        return ApiResponse.success(foodService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<FoodResponse> update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody FoodRequest request
    ) {
        return ApiResponse.success("Food updated", foodService.update(authentication, id, request));
    }

    @PatchMapping("/{id}/availability")
    public ApiResponse<FoodResponse> updateAvailability(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody FoodAvailabilityRequest request
    ) {
        return ApiResponse.success("Availability updated", foodService.updateAvailability(authentication, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable String id) {
        foodService.delete(authentication, id);
        return ApiResponse.success("Food deleted", null);
    }
}
