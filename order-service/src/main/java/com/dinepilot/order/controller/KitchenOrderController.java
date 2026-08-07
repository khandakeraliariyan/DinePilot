package com.dinepilot.order.controller;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.order.dto.OrderResponse;
import com.dinepilot.order.enums.OrderStatus;
import com.dinepilot.order.security.JwtClaims;
import com.dinepilot.order.service.OrderManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/kitchen/orders")
@PreAuthorize("hasRole('KITCHEN')")
public class KitchenOrderController {
    private final OrderManagementService orders;
    public KitchenOrderController(OrderManagementService orders) { this.orders = orders; }

    @GetMapping public ApiResponse<List<OrderResponse>> list(Authentication auth) {
        return ApiResponse.success(orders.kitchenOrders(JwtClaims.restaurantId(auth)));
    }
    @PatchMapping("/{id}/accept") public ApiResponse<OrderResponse> accept(Authentication auth, @PathVariable String id) {
        return change(auth, id, OrderStatus.PREPARING, "Order accepted");
    }
    @PatchMapping("/{id}/preparing") public ApiResponse<OrderResponse> preparing(Authentication auth, @PathVariable String id) {
        return change(auth, id, OrderStatus.PREPARING, "Order preparing");
    }
    @PatchMapping("/{id}/ready") public ApiResponse<OrderResponse> ready(Authentication auth, @PathVariable String id) {
        return change(auth, id, OrderStatus.READY, "Order ready");
    }
    @PatchMapping("/{id}/completed") public ApiResponse<OrderResponse> completed(Authentication auth, @PathVariable String id) {
        return change(auth, id, OrderStatus.COMPLETED, "Order completed");
    }
    private ApiResponse<OrderResponse> change(Authentication auth, String id, OrderStatus status, String message) {
        return ApiResponse.success(message, orders.kitchenTransition(JwtClaims.restaurantId(auth), id, status));
    }
}
