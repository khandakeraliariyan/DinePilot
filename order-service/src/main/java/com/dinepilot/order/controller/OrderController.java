package com.dinepilot.order.controller;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.order.dto.OrderResponse;
import com.dinepilot.order.service.OrderManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {
    private final OrderManagementService orders;
    public OrderController(OrderManagementService orders) { this.orders = orders; }

    @PostMapping public ApiResponse<OrderResponse> place(Authentication auth) {
        return ApiResponse.success("Order placed", orders.place(auth.getName()));
    }
    @GetMapping public ApiResponse<List<OrderResponse>> history(Authentication auth) { return ApiResponse.success(orders.history(auth.getName())); }
    @GetMapping("/{id}") public ApiResponse<OrderResponse> get(Authentication auth, @PathVariable String id) {
        return ApiResponse.success(orders.getForCustomer(auth.getName(), id));
    }
    @PatchMapping("/{id}/cancel") public ApiResponse<OrderResponse> cancel(Authentication auth, @PathVariable String id) {
        return ApiResponse.success("Order cancelled", orders.cancel(auth.getName(), id));
    }
}
