package com.dinepilot.restaurant.service;

import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.restaurant.dto.AddressDto;
import com.dinepilot.restaurant.dto.OpeningHoursDto;
import com.dinepilot.restaurant.dto.RestaurantRequest;
import com.dinepilot.restaurant.dto.RestaurantResponse;
import com.dinepilot.restaurant.entity.Address;
import com.dinepilot.restaurant.entity.OpeningHours;
import com.dinepilot.restaurant.entity.Restaurant;
import com.dinepilot.restaurant.repository.RestaurantRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final OwnershipGuard ownershipGuard;

    public RestaurantService(RestaurantRepository restaurantRepository, OwnershipGuard ownershipGuard) {
        this.restaurantRepository = restaurantRepository;
        this.ownershipGuard = ownershipGuard;
    }

    public RestaurantResponse create(Authentication authentication, RestaurantRequest request) {
        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerId(authentication.getName());
        applyRequest(restaurant, request);
        restaurantRepository.save(restaurant);
        return toResponse(restaurant);
    }

    public RestaurantResponse get(String id) {
        return toResponse(findRestaurant(id));
    }

    public List<RestaurantResponse> list() {
        return restaurantRepository.findAll().stream().map(this::toResponse).toList();
    }

    public RestaurantResponse update(Authentication authentication, String id, RestaurantRequest request) {
        Restaurant restaurant = findRestaurant(id);
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());
        applyRequest(restaurant, request);
        restaurantRepository.save(restaurant);
        return toResponse(restaurant);
    }

    public void delete(Authentication authentication, String id) {
        Restaurant restaurant = findRestaurant(id);
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());
        restaurantRepository.deleteById(id);
    }

    Restaurant findRestaurant(String id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
    }

    private void applyRequest(Restaurant restaurant, RestaurantRequest request) {
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setActive(request.active());

        Address address = new Address();
        AddressDto addressDto = request.address();
        address.setLine1(addressDto.line1());
        address.setCity(addressDto.city());
        address.setState(addressDto.state());
        address.setPostalCode(addressDto.postalCode());
        address.setCountry(addressDto.country());
        restaurant.setAddress(address);

        List<OpeningHours> openingHours = request.openingHours() == null
                ? List.of()
                : request.openingHours().stream().map(this::toEntity).toList();
        restaurant.setOpeningHours(openingHours);
    }

    private OpeningHours toEntity(OpeningHoursDto dto) {
        OpeningHours hours = new OpeningHours();
        hours.setDayOfWeek(dto.dayOfWeek());
        hours.setOpenTime(dto.openTime());
        hours.setCloseTime(dto.closeTime());
        hours.setClosed(dto.closed());
        return hours;
    }

    private RestaurantResponse toResponse(Restaurant restaurant) {
        Address address = restaurant.getAddress();
        AddressDto addressDto = address == null ? null
                : new AddressDto(address.getLine1(), address.getCity(), address.getState(),
                address.getPostalCode(), address.getCountry());

        List<OpeningHoursDto> openingHours = restaurant.getOpeningHours().stream()
                .map(h -> new OpeningHoursDto(h.getDayOfWeek(), h.getOpenTime(), h.getCloseTime(), h.isClosed()))
                .toList();

        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getOwnerId(),
                restaurant.getName(),
                restaurant.getDescription(),
                addressDto,
                openingHours,
                restaurant.isActive()
        );
    }
}
