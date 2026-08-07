package com.dinepilot.order.controller;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.order.dto.CartItemQuantityRequest;
import com.dinepilot.order.dto.CartItemRequest;
import com.dinepilot.order.dto.CartResponse;
import com.dinepilot.order.service.CartService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {
    private final CartService carts;
    public CartController(CartService carts) { this.carts = carts; }

    @GetMapping public ApiResponse<CartResponse> get(Authentication auth) { return ApiResponse.success(carts.get(auth.getName())); }
    @PostMapping("/items") public ApiResponse<CartResponse> add(Authentication auth, @Valid @RequestBody CartItemRequest r) {
        return ApiResponse.success("Item added", carts.add(auth.getName(), r.foodId(), r.quantity()));
    }
    @PutMapping("/items/{foodId}") public ApiResponse<CartResponse> update(Authentication auth, @PathVariable String foodId,
            @Valid @RequestBody CartItemQuantityRequest r) {
        return ApiResponse.success("Item updated", carts.update(auth.getName(), foodId, r.quantity()));
    }
    @DeleteMapping("/items/{foodId}") public ApiResponse<CartResponse> remove(Authentication auth, @PathVariable String foodId) {
        return ApiResponse.success("Item removed", carts.remove(auth.getName(), foodId));
    }
}
