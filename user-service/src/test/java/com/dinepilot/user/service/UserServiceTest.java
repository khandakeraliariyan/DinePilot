package com.dinepilot.user.service;

import com.dinepilot.common.enums.Role;
import com.dinepilot.common.exception.ConflictException;
import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.user.dto.UpdateProfileRequest;
import com.dinepilot.user.dto.UserProfileResponse;
import com.dinepilot.user.entity.User;
import com.dinepilot.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String USER_ID = "user-1";

    @Mock
    private UserRepository users;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(users);
    }

    @Test
    void returnsRestaurantAssignmentInAKitchenProfile() {
        User kitchen = user(Role.KITCHEN);
        kitchen.setRestaurantId("restaurant-1");
        when(users.findById(USER_ID)).thenReturn(Optional.of(kitchen));

        UserProfileResponse response = service.getProfile(USER_ID);

        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.role()).isEqualTo(Role.KITCHEN);
        assertThat(response.restaurantId()).isEqualTo("restaurant-1");
    }

    @Test
    void returnsNullRestaurantForAnUnassignedCustomer() {
        User customer = user(Role.CUSTOMER);
        when(users.findById(USER_ID)).thenReturn(Optional.of(customer));

        UserProfileResponse response = service.getProfile(USER_ID);

        assertThat(response.restaurantId()).isNull();
    }

    @Test
    void updatesEditableProfileFieldsWithoutChangingKitchenScope() {
        User kitchen = user(Role.KITCHEN);
        kitchen.setRestaurantId("restaurant-1");
        when(users.findById(USER_ID)).thenReturn(Optional.of(kitchen));
        when(users.save(kitchen)).thenReturn(kitchen);

        UserProfileResponse response = service.updateProfile(
                USER_ID,
                new UpdateProfileRequest("Updated Cook", "+8801000000000")
        );

        assertThat(response.fullName()).isEqualTo("Updated Cook");
        assertThat(response.phone()).isEqualTo("+8801000000000");
        assertThat(response.role()).isEqualTo(Role.KITCHEN);
        assertThat(response.restaurantId()).isEqualTo("restaurant-1");
        verify(users).save(kitchen);
    }

    @Test
    void assignsARestaurantToAKitchenUser() {
        User kitchen = user(Role.KITCHEN);
        when(users.findById(USER_ID)).thenReturn(Optional.of(kitchen));
        when(users.save(kitchen)).thenReturn(kitchen);

        UserProfileResponse response = service.assignKitchenRestaurant(USER_ID, "restaurant-2");

        assertThat(kitchen.getRestaurantId()).isEqualTo("restaurant-2");
        assertThat(response.restaurantId()).isEqualTo("restaurant-2");
        verify(users).save(kitchen);
    }

    @Test
    void canMoveAKitchenUserToAnotherRestaurant() {
        User kitchen = user(Role.KITCHEN);
        kitchen.setRestaurantId("restaurant-1");
        when(users.findById(USER_ID)).thenReturn(Optional.of(kitchen));
        when(users.save(kitchen)).thenReturn(kitchen);

        UserProfileResponse response = service.assignKitchenRestaurant(USER_ID, "restaurant-2");

        assertThat(response.restaurantId()).isEqualTo("restaurant-2");
    }

    @Test
    void rejectsRestaurantAssignmentForACustomer() {
        User customer = user(Role.CUSTOMER);
        when(users.findById(USER_ID)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.assignKitchenRestaurant(USER_ID, "restaurant-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Restaurant assignment is only valid for KITCHEN users");

        assertThat(customer.getRestaurantId()).isNull();
        verify(users, never()).save(any());
    }

    @Test
    void rejectsRestaurantAssignmentForARestaurantAdmin() {
        User admin = user(Role.RESTAURANT_ADMIN);
        when(users.findById(USER_ID)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.assignKitchenRestaurant(USER_ID, "restaurant-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Restaurant assignment is only valid for KITCHEN users");

        verify(users, never()).save(any());
    }

    @Test
    void reportsAMissingUserDuringAssignment() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignKitchenRestaurant(USER_ID, "restaurant-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void reportsAMissingUserDuringProfileRead() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    private User user(Role role) {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("cook@example.com");
        user.setFullName("Kitchen Cook");
        user.setPhone("+8801999999999");
        user.setRole(role);
        return user;
    }
}
