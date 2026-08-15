package com.dinepilot.order.service;

import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ForbiddenException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.order.dto.OrderResponse;
import com.dinepilot.order.entity.Cart;
import com.dinepilot.order.entity.CartItem;
import com.dinepilot.order.entity.Order;
import com.dinepilot.order.entity.OrderItem;
import com.dinepilot.order.enums.OrderStatus;
import com.dinepilot.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderManagementServiceTest {

    private static final String USER_ID = "user-1";
    private static final String OTHER_USER_ID = "user-2";
    private static final String RESTAURANT_ID = "restaurant-1";
    private static final String OTHER_RESTAURANT_ID = "restaurant-2";
    private static final String ORDER_ID = "order-1";

    @Mock
    private OrderRepository orders;

    @Mock
    private CartService cartService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OrderManagementService service;

    @BeforeEach
    void setUp() {
        service = new OrderManagementService(orders, cartService, rabbitTemplate);
    }

    @Nested
    @DisplayName("placing an order")
    class PlaceOrder {

        @Test
        void snapshotsEveryCartLineAndCalculatesTheTotal() {
            Cart cart = cart(
                    cartItem("food-1", "House Burger", "8.50", 2),
                    cartItem("food-2", "Tea", "2.25", 3)
            );
            when(cartService.findExisting(USER_ID)).thenReturn(cart);
            when(orders.save(any(Order.class))).thenAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setId(ORDER_ID);
                saved.setCreatedAt(Instant.parse("2026-08-08T10:15:30Z"));
                return saved;
            });

            OrderResponse response = service.place(USER_ID);

            assertThat(response.id()).isEqualTo(ORDER_ID);
            assertThat(response.userId()).isEqualTo(USER_ID);
            assertThat(response.restaurantId()).isEqualTo(RESTAURANT_ID);
            assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
            assertThat(response.total()).isEqualByComparingTo("23.75");
            assertThat(response.items()).hasSize(2);
            assertThat(response.items().get(0)).satisfies(line -> {
                assertThat(line.foodId()).isEqualTo("food-1");
                assertThat(line.foodName()).isEqualTo("House Burger");
                assertThat(line.unitPrice()).isEqualByComparingTo("8.50");
                assertThat(line.quantity()).isEqualTo(2);
                assertThat(line.lineTotal()).isEqualByComparingTo("17.00");
            });
            assertThat(response.items().get(1).lineTotal()).isEqualByComparingTo("6.75");
        }

        @Test
        void createsIndependentOrderItemObjects() {
            CartItem cartItem = cartItem("food-1", "Burger", "8.50", 1);
            Cart cart = cart(cartItem);
            when(cartService.findExisting(USER_ID)).thenReturn(cart);
            when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.place(USER_ID);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orders).save(captor.capture());
            OrderItem snapshot = captor.getValue().getItems().get(0);

            assertThat((Object) snapshot).isNotSameAs(cartItem);
            assertThat(snapshot.getFoodName()).isEqualTo(cartItem.getFoodName());
            assertThat(snapshot.getUnitPrice()).isEqualByComparingTo(cartItem.getUnitPrice());
        }

        @Test
        void savesTheOrderBeforeClearingTheCart() {
            Cart cart = cart(cartItem("food-1", "Burger", "8.50", 1));
            when(cartService.findExisting(USER_ID)).thenReturn(cart);
            when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.place(USER_ID);

            InOrder sequence = inOrder(orders, cartService);
            sequence.verify(orders).save(any(Order.class));
            sequence.verify(cartService).clear(cart);
        }

        @Test
        void leavesTheCartUntouchedWhenSavingFails() {
            Cart cart = cart(cartItem("food-1", "Burger", "8.50", 1));
            when(cartService.findExisting(USER_ID)).thenReturn(cart);
            when(orders.save(any(Order.class))).thenThrow(new RuntimeException("database unavailable"));

            assertThatThrownBy(() -> service.place(USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("database unavailable");

            verify(cartService, never()).clear(any());
        }

        @Test
        void rejectsAnEmptyStoredCart() {
            Cart cart = cart();
            when(cartService.findExisting(USER_ID)).thenReturn(cart);

            assertThatThrownBy(() -> service.place(USER_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Cannot place an empty cart");

            verify(orders, never()).save(any());
            verify(cartService, never()).clear(any());
        }

        @Test
        void propagatesTheMissingCartError() {
            when(cartService.findExisting(USER_ID)).thenThrow(new ResourceNotFoundException("Cart is empty"));

            assertThatThrownBy(() -> service.place(USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cart is empty");

            verify(orders, never()).save(any());
        }
    }

    @Nested
    @DisplayName("customer order access")
    class CustomerAccess {

        @Test
        void returnsNewestFirstHistoryFromTheRepository() {
            Order newest = order("order-2", USER_ID, RESTAURANT_ID, OrderStatus.READY);
            Order older = order("order-1", USER_ID, RESTAURANT_ID, OrderStatus.COMPLETED);
            when(orders.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of(newest, older));

            List<OrderResponse> history = service.history(USER_ID);

            assertThat(history).extracting(OrderResponse::id).containsExactly("order-2", "order-1");
            verify(orders).findByUserIdOrderByCreatedAtDesc(USER_ID);
        }

        @Test
        void returnsAnOwnedOrder() {
            Order order = order(ORDER_ID, USER_ID, RESTAURANT_ID, OrderStatus.PREPARING);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));

            OrderResponse response = service.getForCustomer(USER_ID, ORDER_ID);

            assertThat(response.id()).isEqualTo(ORDER_ID);
            assertThat(response.status()).isEqualTo(OrderStatus.PREPARING);
        }

        @Test
        void hidesAnotherCustomersOrder() {
            Order order = order(ORDER_ID, OTHER_USER_ID, RESTAURANT_ID, OrderStatus.PLACED);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.getForCustomer(USER_ID, ORDER_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("This order belongs to another customer");
        }

        @Test
        void reportsAMissingOrder() {
            when(orders.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getForCustomer(USER_ID, ORDER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order not found");
        }

        @Test
        void allowsTheOwnerToCancelAPlacedOrder() {
            Order order = order(ORDER_ID, USER_ID, RESTAURANT_ID, OrderStatus.PLACED);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orders.save(order)).thenReturn(order);

            OrderResponse response = service.cancel(USER_ID, ORDER_ID);

            assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orders).save(order);
        }

        @Test
        void rejectsCancellationByAnotherCustomer() {
            Order order = order(ORDER_ID, OTHER_USER_ID, RESTAURANT_ID, OrderStatus.PLACED);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.cancel(USER_ID, ORDER_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("This order belongs to another customer");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
            verify(orders, never()).save(any());
        }

        @Test
        void rejectsCancellationAfterPreparationStarts() {
            Order order = order(ORDER_ID, USER_ID, RESTAURANT_ID, OrderStatus.PREPARING);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.cancel(USER_ID, ORDER_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Order must be PLACED before it can become CANCELLED");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
            verify(orders, never()).save(any());
        }

        @Test
        void rejectsRepeatedCancellation() {
            Order order = order(ORDER_ID, USER_ID, RESTAURANT_ID, OrderStatus.CANCELLED);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.cancel(USER_ID, ORDER_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Order must be PLACED before it can become CANCELLED");
        }
    }

    @Nested
    @DisplayName("kitchen workflow")
    class KitchenWorkflow {

        @Test
        void listsOnlyTheRepositoryResultForTheAssignedRestaurant() {
            Order first = order("order-1", USER_ID, RESTAURANT_ID, OrderStatus.PLACED);
            Order second = order("order-2", OTHER_USER_ID, RESTAURANT_ID, OrderStatus.READY);
            when(orders.findByRestaurantIdOrderByCreatedAtDesc(RESTAURANT_ID))
                    .thenReturn(List.of(first, second));

            List<OrderResponse> response = service.kitchenOrders(RESTAURANT_ID);

            assertThat(response).extracting(OrderResponse::id).containsExactly("order-1", "order-2");
            verify(orders).findByRestaurantIdOrderByCreatedAtDesc(RESTAURANT_ID);
        }

        @Test
        void acceptsAPlacedOrder() {
            assertTransition(OrderStatus.PLACED, OrderStatus.PREPARING);
        }

        @Test
        void marksAPreparingOrderReady() {
            assertTransition(OrderStatus.PREPARING, OrderStatus.READY);
        }

        @Test
        void completesAReadyOrder() {
            assertTransition(OrderStatus.READY, OrderStatus.COMPLETED);
        }

        @Test
        void rejectsAnOrderFromAnotherRestaurant() {
            Order order = order(ORDER_ID, USER_ID, OTHER_RESTAURANT_ID, OrderStatus.PLACED);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.kitchenTransition(
                    RESTAURANT_ID, ORDER_ID, OrderStatus.PREPARING))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Order is outside your restaurant");

            verify(orders, never()).save(any());
        }

        @Test
        void rejectsSkippingFromPlacedDirectlyToReady() {
            Order order = order(ORDER_ID, USER_ID, RESTAURANT_ID, OrderStatus.PLACED);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.kitchenTransition(
                    RESTAURANT_ID, ORDER_ID, OrderStatus.READY))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Order must be PREPARING before it can become READY");

            verify(orders, never()).save(any());
        }

        @Test
        void rejectsCompletingBeforeReady() {
            Order order = order(ORDER_ID, USER_ID, RESTAURANT_ID, OrderStatus.PREPARING);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.kitchenTransition(
                    RESTAURANT_ID, ORDER_ID, OrderStatus.COMPLETED))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Order must be READY before it can become COMPLETED");
        }

        @Test
        void rejectsKitchenCancellationAsAnUnsupportedTarget() {
            Order order = order(ORDER_ID, USER_ID, RESTAURANT_ID, OrderStatus.PLACED);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.kitchenTransition(
                    RESTAURANT_ID, ORDER_ID, OrderStatus.CANCELLED))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Unsupported kitchen status transition");
        }

        @Test
        void rejectsKitchenPlacedAsATarget() {
            Order order = order(ORDER_ID, USER_ID, RESTAURANT_ID, OrderStatus.PREPARING);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.kitchenTransition(
                    RESTAURANT_ID, ORDER_ID, OrderStatus.PLACED))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Unsupported kitchen status transition");
        }

        @Test
        void rejectsRepeatingThePreparingTransition() {
            Order order = order(ORDER_ID, USER_ID, RESTAURANT_ID, OrderStatus.PREPARING);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.kitchenTransition(
                    RESTAURANT_ID, ORDER_ID, OrderStatus.PREPARING))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Order must be PLACED before it can become PREPARING");
        }

        private void assertTransition(OrderStatus current, OrderStatus target) {
            Order order = order(ORDER_ID, USER_ID, RESTAURANT_ID, current);
            when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(orders.save(order)).thenReturn(order);

            OrderResponse response = service.kitchenTransition(RESTAURANT_ID, ORDER_ID, target);

            assertThat(response.status()).isEqualTo(target);
            assertThat(order.getStatus()).isEqualTo(target);
            verify(orders).save(order);
        }
    }

    private Cart cart(CartItem... items) {
        Cart cart = new Cart();
        cart.setId("cart-1");
        cart.setUserId(USER_ID);
        cart.setRestaurantId(RESTAURANT_ID);
        cart.setItems(new ArrayList<>(List.of(items)));
        return cart;
    }

    private CartItem cartItem(String foodId, String foodName, String unitPrice, int quantity) {
        CartItem item = new CartItem();
        item.setFoodId(foodId);
        item.setRestaurantId(RESTAURANT_ID);
        item.setFoodName(foodName);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setQuantity(quantity);
        return item;
    }

    private Order order(
            String id,
            String userId,
            String restaurantId,
            OrderStatus status
    ) {
        OrderItem item = new OrderItem();
        item.setFoodId("food-1");
        item.setFoodName("Burger");
        item.setUnitPrice(new BigDecimal("8.50"));
        item.setQuantity(2);
        item.setLineTotal(new BigDecimal("17.00"));

        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setRestaurantId(restaurantId);
        order.setStatus(status);
        order.setItems(List.of(item));
        order.setTotal(new BigDecimal("17.00"));
        order.setCreatedAt(Instant.parse("2026-08-08T10:15:30Z"));
        order.setUpdatedAt(Instant.parse("2026-08-08T10:20:30Z"));
        return order;
    }
}
