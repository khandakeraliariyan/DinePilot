package com.dinepilot.reservation.controller;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.reservation.dto.ReservationResponse;
import com.dinepilot.reservation.service.ReservationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations/restaurant")
@PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'SUPER_ADMIN')")
public class RestaurantReservationController {
    private final ReservationService reservations;
    public RestaurantReservationController(ReservationService reservations) { this.reservations = reservations; }

    @GetMapping("/{restaurantId}") public ApiResponse<List<ReservationResponse>> list(Authentication auth, @PathVariable String restaurantId) {
        return ApiResponse.success(reservations.forRestaurant(auth, restaurantId));
    }
    @PatchMapping("/{id}/confirm") public ApiResponse<ReservationResponse> confirm(Authentication auth, @PathVariable String id) {
        return ApiResponse.success("Reservation confirmed", reservations.confirm(auth, id));
    }
    @PatchMapping("/{id}/complete") public ApiResponse<ReservationResponse> complete(Authentication auth, @PathVariable String id) {
        return ApiResponse.success("Reservation completed", reservations.completeByRestaurant(auth, id));
    }
    @PatchMapping("/{id}/cancel") public ApiResponse<ReservationResponse> cancel(Authentication auth, @PathVariable String id) {
        return ApiResponse.success("Reservation cancelled", reservations.cancelByRestaurant(auth, id));
    }
}
