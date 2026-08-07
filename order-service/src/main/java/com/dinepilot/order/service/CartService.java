package com.dinepilot.order.service;

import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.order.client.RestaurantMenuClient;
import com.dinepilot.order.dto.CartResponse;
import com.dinepilot.order.entity.Cart;
import com.dinepilot.order.entity.CartItem;
import com.dinepilot.order.repository.CartRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;

@Service
public class CartService {
    private final CartRepository carts;
    private final RestaurantMenuClient menuClient;

    public CartService(CartRepository carts, RestaurantMenuClient menuClient) {
        this.carts = carts;
        this.menuClient = menuClient;
    }

    public CartResponse get(String userId) { return toResponse(findOrNew(userId)); }

    public CartResponse add(String userId, String foodId, int quantity) {
        var food = menuClient.getAvailableFood(foodId);
        Cart cart = findOrNew(userId);
        if (cart.getRestaurantId() != null && !cart.getRestaurantId().equals(food.restaurantId())) {
            throw new ConflictException("A cart can only contain items from one restaurant");
        }
        CartItem existing = cart.getItems().stream().filter(i -> i.getFoodId().equals(foodId)).findFirst().orElse(null);
        if (existing == null) {
            existing = new CartItem();
            existing.setFoodId(food.id());
            existing.setRestaurantId(food.restaurantId());
            cart.getItems().add(existing);
        }
        existing.setFoodName(food.name());
        existing.setUnitPrice(food.price());
        existing.setQuantity(existing.getQuantity() + quantity);
        cart.setRestaurantId(food.restaurantId());
        return toResponse(carts.save(cart));
    }

    public CartResponse update(String userId, String foodId, int quantity) {
        Cart cart = findExisting(userId);
        CartItem item = cart.getItems().stream().filter(i -> i.getFoodId().equals(foodId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        item.setQuantity(quantity);
        return toResponse(carts.save(cart));
    }

    public CartResponse remove(String userId, String foodId) {
        Cart cart = findExisting(userId);
        if (!cart.getItems().removeIf(i -> i.getFoodId().equals(foodId))) {
            throw new ResourceNotFoundException("Cart item not found");
        }
        if (cart.getItems().isEmpty()) cart.setRestaurantId(null);
        return toResponse(carts.save(cart));
    }

    Cart findExisting(String userId) {
        return carts.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));
    }

    void clear(Cart cart) { carts.delete(cart); }

    private Cart findOrNew(String userId) {
        return carts.findByUserId(userId).orElseGet(() -> {
            Cart cart = new Cart(); cart.setUserId(userId); cart.setItems(new ArrayList<>()); return cart;
        });
    }

    private CartResponse toResponse(Cart cart) {
        var items = cart.getItems().stream().map(i -> new CartResponse.Item(i.getFoodId(), i.getRestaurantId(),
                i.getFoodName(), i.getUnitPrice(), i.getQuantity(),
                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))).toList();
        BigDecimal total = items.stream().map(CartResponse.Item::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), cart.getRestaurantId(), items, total);
    }
}
