package com.dinepilot.reservation.controller;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.reservation.dto.ReservationRequest;
import com.dinepilot.reservation.dto.ReservationResponse;
import com.dinepilot.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@PreAuthorize("hasRole('CUSTOMER')")
public class ReservationController {
    private final ReservationService reservations;
    public ReservationController(ReservationService reservations) { this.reservations = reservations; }

    @PostMapping public ApiResponse<ReservationResponse> book(Authentication auth, @Valid @RequestBody ReservationRequest request) {
        return ApiResponse.success("Reservation requested", reservations.book(auth.getName(), request));
    }
    @GetMapping public ApiResponse<List<ReservationResponse>> history(Authentication auth) {
        return ApiResponse.success(reservations.history(auth.getName()));
    }
    @GetMapping("/{id}") public ApiResponse<ReservationResponse> get(Authentication auth, @PathVariable String id) {
        return ApiResponse.success(reservations.getForCustomer(auth.getName(), id));
    }
    @PatchMapping("/{id}/cancel") public ApiResponse<ReservationResponse> cancel(Authentication auth, @PathVariable String id) {
        return ApiResponse.success("Reservation cancelled", reservations.cancelByCustomer(auth.getName(), id));
    }
}
