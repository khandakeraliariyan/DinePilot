package com.dinepilot.order.service;

import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.order.client.RestaurantMenuClient;
import com.dinepilot.order.dto.CartResponse;
import com.dinepilot.order.entity.Cart;
import com.dinepilot.order.entity.CartItem;
import com.dinepilot.order.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final String USER_ID = "user-1";
    private static final String RESTAURANT_ID = "restaurant-1";
    private static final String FOOD_ID = "food-1";

    @Mock
    private CartRepository carts;

    @Mock
    private RestaurantMenuClient menuClient;

    private CartService service;

    @BeforeEach
    void setUp() {
        service = new CartService(carts, menuClient);
    }

    @Nested
    @DisplayName("viewing a cart")
    class GetCart {

        @Test
        void returnsAnEmptyUnpersistedCartForANewCustomer() {
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.empty());

            CartResponse response = service.get(USER_ID);

            assertThat(response.id()).isNull();
            assertThat(response.restaurantId()).isNull();
            assertThat(response.items()).isEmpty();
            assertThat(response.total()).isEqualByComparingTo("0");
            verify(carts, never()).save(any());
        }

        @Test
        void mapsStoredLinesAndCalculatesTheCartTotal() {
            Cart cart = cartWith(
                    item("food-1", "Burger", "8.50", 2),
                    item("food-2", "Tea", "2.25", 3)
            );
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

            CartResponse response = service.get(USER_ID);

            assertThat(response.items()).hasSize(2);
            assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("17.00");
            assertThat(response.items().get(1).lineTotal()).isEqualByComparingTo("6.75");
            assertThat(response.total()).isEqualByComparingTo("23.75");
        }

        @Test
        void preservesMoneyScaleIndependentlyOfComparison() {
            Cart cart = cartWith(item(FOOD_ID, "Coffee", "3.125", 2));
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

            CartResponse response = service.get(USER_ID);

            assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("6.250");
            assertThat(response.total()).isEqualByComparingTo("6.250");
        }
    }

    @Nested
    @DisplayName("adding an item")
    class AddItem {

        @Test
        void createsTheFirstLineFromLiveMenuData() {
            when(menuClient.getAvailableFood(FOOD_ID)).thenReturn(menuItem(
                    FOOD_ID, RESTAURANT_ID, "House Burger", "8.50", true));
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(carts.save(any(Cart.class))).thenAnswer(invocation -> {
                Cart saved = invocation.getArgument(0);
                saved.setId("cart-1");
                return saved;
            });

            CartResponse response = service.add(USER_ID, FOOD_ID, 2);

            assertThat(response.id()).isEqualTo("cart-1");
            assertThat(response.restaurantId()).isEqualTo(RESTAURANT_ID);
            assertThat(response.items()).singleElement().satisfies(line -> {
                assertThat(line.foodId()).isEqualTo(FOOD_ID);
                assertThat(line.restaurantId()).isEqualTo(RESTAURANT_ID);
                assertThat(line.foodName()).isEqualTo("House Burger");
                assertThat(line.unitPrice()).isEqualByComparingTo("8.50");
                assertThat(line.quantity()).isEqualTo(2);
                assertThat(line.lineTotal()).isEqualByComparingTo("17.00");
            });
            assertThat(response.total()).isEqualByComparingTo("17.00");
        }

        @Test
        void assignsTheAuthenticatedCustomerToANewCart() {
            when(menuClient.getAvailableFood(FOOD_ID)).thenReturn(menuItem(
                    FOOD_ID, RESTAURANT_ID, "Soup", "5.00", true));
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(carts.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.add(USER_ID, FOOD_ID, 1);

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(carts).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        void incrementsAnExistingLineInsteadOfDuplicatingIt() {
            Cart cart = cartWith(item(FOOD_ID, "Burger", "8.50", 2));
            when(menuClient.getAvailableFood(FOOD_ID)).thenReturn(menuItem(
                    FOOD_ID, RESTAURANT_ID, "Burger", "8.50", true));
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(carts.save(cart)).thenReturn(cart);

            CartResponse response = service.add(USER_ID, FOOD_ID, 3);

            assertThat(response.items()).hasSize(1);
            assertThat(response.items().get(0).quantity()).isEqualTo(5);
            assertThat(response.total()).isEqualByComparingTo("42.50");
        }

        @Test
        void refreshesNameAndPriceWhenAnExistingItemIsAddedAgain() {
            Cart cart = cartWith(item(FOOD_ID, "Old Burger", "7.00", 1));
            when(menuClient.getAvailableFood(FOOD_ID)).thenReturn(menuItem(
                    FOOD_ID, RESTAURANT_ID, "Classic Burger", "9.25", true));
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(carts.save(cart)).thenReturn(cart);

            CartResponse response = service.add(USER_ID, FOOD_ID, 1);

            CartResponse.Item line = response.items().get(0);
            assertThat(line.foodName()).isEqualTo("Classic Burger");
            assertThat(line.unitPrice()).isEqualByComparingTo("9.25");
            assertThat(line.quantity()).isEqualTo(2);
            assertThat(line.lineTotal()).isEqualByComparingTo("18.50");
        }

        @Test
        void allowsDifferentFoodsFromTheSameRestaurant() {
            Cart cart = cartWith(item(FOOD_ID, "Burger", "8.50", 1));
            when(menuClient.getAvailableFood("food-2")).thenReturn(menuItem(
                    "food-2", RESTAURANT_ID, "Fries", "3.00", true));
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(carts.save(cart)).thenReturn(cart);

            CartResponse response = service.add(USER_ID, "food-2", 2);

            assertThat(response.items()).hasSize(2);
            assertThat(response.total()).isEqualByComparingTo("14.50");
        }

        @Test
        void rejectsAnItemFromASecondRestaurant() {
            Cart cart = cartWith(item(FOOD_ID, "Burger", "8.50", 1));
            when(menuClient.getAvailableFood("food-2")).thenReturn(menuItem(
                    "food-2", "restaurant-2", "Pizza", "11.00", true));
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

            assertThatThrownBy(() -> service.add(USER_ID, "food-2", 1))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("A cart can only contain items from one restaurant");

            assertThat(cart.getItems()).hasSize(1);
            verify(carts, never()).save(any());
        }

        @Test
        void asksTheMenuClientBeforeLoadingOrChangingTheCart() {
            when(menuClient.getAvailableFood(FOOD_ID)).thenThrow(
                    new ConflictException("Food is currently unavailable"));

            assertThatThrownBy(() -> service.add(USER_ID, FOOD_ID, 1))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Food is currently unavailable");

            verify(carts, never()).findByUserId(any());
            verify(carts, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updating an item")
    class UpdateItem {

        @Test
        void replacesTheQuantityAndRecalculatesTotals() {
            Cart cart = cartWith(item(FOOD_ID, "Burger", "8.50", 2));
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(carts.save(cart)).thenReturn(cart);

            CartResponse response = service.update(USER_ID, FOOD_ID, 4);

            assertThat(response.items().get(0).quantity()).isEqualTo(4);
            assertThat(response.total()).isEqualByComparingTo("34.00");
            verify(menuClient, never()).getAvailableFood(any());
        }

        @Test
        void rejectsAnUpdateWhenTheCustomerHasNoCart() {
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(USER_ID, FOOD_ID, 2))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cart is empty");

            verify(carts, never()).save(any());
        }

        @Test
        void rejectsAnUpdateWhenTheFoodIsNotInTheCart() {
            Cart cart = cartWith(item(FOOD_ID, "Burger", "8.50", 1));
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

            assertThatThrownBy(() -> service.update(USER_ID, "missing-food", 2))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cart item not found");

            verify(carts, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removing an item")
    class RemoveItem {

        @Test
        void removesOnlyTheRequestedLine() {
            Cart cart = cartWith(
                    item(FOOD_ID, "Burger", "8.50", 1),
                    item("food-2", "Tea", "2.25", 2)
            );
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(carts.save(cart)).thenReturn(cart);

            CartResponse response = service.remove(USER_ID, FOOD_ID);

            assertThat(response.items()).singleElement().satisfies(
                    line -> assertThat(line.foodId()).isEqualTo("food-2"));
            assertThat(response.restaurantId()).isEqualTo(RESTAURANT_ID);
            assertThat(response.total()).isEqualByComparingTo("4.50");
        }

        @Test
        void clearsRestaurantScopeAfterRemovingTheLastLine() {
            Cart cart = cartWith(item(FOOD_ID, "Burger", "8.50", 1));
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
            when(carts.save(cart)).thenReturn(cart);

            CartResponse response = service.remove(USER_ID, FOOD_ID);

            assertThat(response.items()).isEmpty();
            assertThat(response.restaurantId()).isNull();
            assertThat(response.total()).isEqualByComparingTo("0");
        }

        @Test
        void rejectsRemovalWhenTheCustomerHasNoCart() {
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.remove(USER_ID, FOOD_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cart is empty");
        }

        @Test
        void rejectsRemovalWhenTheFoodIsNotInTheCart() {
            Cart cart = cartWith(item(FOOD_ID, "Burger", "8.50", 1));
            when(carts.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

            assertThatThrownBy(() -> service.remove(USER_ID, "missing-food"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cart item not found");

            assertThat(cart.getItems()).hasSize(1);
            verify(carts, never()).save(any());
        }
    }

    private Cart cartWith(CartItem... items) {
        Cart cart = new Cart();
        cart.setId("cart-1");
        cart.setUserId(USER_ID);
        cart.setRestaurantId(RESTAURANT_ID);
        cart.setItems(new ArrayList<>(List.of(items)));
        return cart;
    }

    private CartItem item(String foodId, String name, String price, int quantity) {
        CartItem item = new CartItem();
        item.setFoodId(foodId);
        item.setRestaurantId(RESTAURANT_ID);
        item.setFoodName(name);
        item.setUnitPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        return item;
    }

    private RestaurantMenuClient.MenuItem menuItem(
            String id,
            String restaurantId,
            String name,
            String price,
            boolean available
    ) {
        return new RestaurantMenuClient.MenuItem(
                id,
                restaurantId,
                "category-1",
                name,
                "Description",
                new BigDecimal(price),
                available
        );
    }
}
