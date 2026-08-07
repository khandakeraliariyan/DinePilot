package com.dinepilot.user.controller;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.user.dto.UpdateProfileRequest;
import com.dinepilot.user.dto.KitchenRestaurantAssignmentRequest;
import com.dinepilot.user.dto.UserProfileResponse;
import com.dinepilot.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getProfile(Authentication authentication) {
        return ApiResponse.success(userService.getProfile(authentication.getName()));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success("Profile updated", userService.updateProfile(authentication.getName(), request));
    }

    @PatchMapping("/{userId}/kitchen-restaurant")
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<UserProfileResponse> assignKitchenRestaurant(
            @PathVariable String userId,
            @Valid @RequestBody KitchenRestaurantAssignmentRequest request
    ) {
        return ApiResponse.success("Kitchen restaurant assigned",
                userService.assignKitchenRestaurant(userId, request.restaurantId()));
    }
}
