package com.dinepilot.order.service;

import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ForbiddenException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.order.dto.OrderResponse;
import com.dinepilot.order.entity.Cart;
import com.dinepilot.order.entity.Order;
import com.dinepilot.order.entity.OrderItem;
import com.dinepilot.order.enums.OrderStatus;
import com.dinepilot.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderManagementService {
    private final OrderRepository orders;
    private final CartService cartService;

    public OrderManagementService(OrderRepository orders, CartService cartService) {
        this.orders = orders;
        this.cartService = cartService;
    }

    public OrderResponse place(String userId) {
        Cart cart = cartService.findExisting(userId);
        if (cart.getItems().isEmpty()) throw new ConflictException("Cannot place an empty cart");
        Order order = new Order();
        order.setUserId(userId);
        order.setRestaurantId(cart.getRestaurantId());
        order.setStatus(OrderStatus.PLACED);
        order.setItems(cart.getItems().stream().map(i -> {
            OrderItem item = new OrderItem();
            item.setFoodId(i.getFoodId()); item.setFoodName(i.getFoodName()); item.setUnitPrice(i.getUnitPrice());
            item.setQuantity(i.getQuantity()); item.setLineTotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
            return item;
        }).toList());
        order.setTotal(order.getItems().stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        Order saved = orders.save(order);
        cartService.clear(cart);
        return toResponse(saved);
    }

    public List<OrderResponse> history(String userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    public OrderResponse getForCustomer(String userId, String orderId) {
        Order order = find(orderId);
        if (!order.getUserId().equals(userId)) throw new ForbiddenException("This order belongs to another customer");
        return toResponse(order);
    }

    public OrderResponse cancel(String userId, String orderId) {
        Order order = find(orderId);
        if (!order.getUserId().equals(userId)) throw new ForbiddenException("This order belongs to another customer");
        transition(order, OrderStatus.PLACED, OrderStatus.CANCELLED);
        return toResponse(orders.save(order));
    }

    public List<OrderResponse> kitchenOrders(String restaurantId) {
        return orders.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream().map(this::toResponse).toList();
    }

    public OrderResponse kitchenTransition(String restaurantId, String orderId, OrderStatus target) {
        Order order = find(orderId);
        if (!order.getRestaurantId().equals(restaurantId)) throw new ForbiddenException("Order is outside your restaurant");
        OrderStatus required = switch (target) {
            case PREPARING -> OrderStatus.PLACED;
            case READY -> OrderStatus.PREPARING;
            case COMPLETED -> OrderStatus.READY;
            default -> throw new ConflictException("Unsupported kitchen status transition");
        };
        transition(order, required, target);
        return toResponse(orders.save(order));
    }

    private void transition(Order order, OrderStatus required, OrderStatus target) {
        if (order.getStatus() != required) {
            throw new ConflictException("Order must be " + required + " before it can become " + target);
        }
        order.setStatus(target);
    }

    private Order find(String id) {
        return orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private OrderResponse toResponse(Order order) {
        var items = order.getItems().stream().map(i -> new OrderResponse.Item(i.getFoodId(), i.getFoodName(),
                i.getUnitPrice(), i.getQuantity(), i.getLineTotal())).toList();
        return new OrderResponse(order.getId(), order.getUserId(), order.getRestaurantId(), order.getStatus(),
                items, order.getTotal(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
