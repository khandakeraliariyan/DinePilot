package com.dinepilot.restaurant.service;

import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.restaurant.dto.TableRequest;
import com.dinepilot.restaurant.dto.TableResponse;
import com.dinepilot.restaurant.dto.TableStatusUpdateRequest;
import com.dinepilot.restaurant.entity.Restaurant;
import com.dinepilot.restaurant.entity.RestaurantTable;
import com.dinepilot.restaurant.repository.RestaurantTableRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final RestaurantService restaurantService;
    private final OwnershipGuard ownershipGuard;

    public TableService(
            RestaurantTableRepository tableRepository,
            RestaurantService restaurantService,
            OwnershipGuard ownershipGuard
    ) {
        this.tableRepository = tableRepository;
        this.restaurantService = restaurantService;
        this.ownershipGuard = ownershipGuard;
    }

    public TableResponse create(Authentication authentication, TableRequest request) {
        Restaurant restaurant = restaurantService.findRestaurant(request.restaurantId());
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());

        RestaurantTable table = new RestaurantTable();
        table.setRestaurantId(request.restaurantId());
        table.setTableNumber(request.tableNumber());
        table.setCapacity(request.capacity());
        tableRepository.save(table);
        return toResponse(table);
    }

    public TableResponse get(String id) {
        return toResponse(findTable(id));
    }

    public List<TableResponse> list(String restaurantId) {
        List<RestaurantTable> tables = restaurantId == null
                ? tableRepository.findAll()
                : tableRepository.findByRestaurantId(restaurantId);
        return tables.stream().map(this::toResponse).toList();
    }

    public TableResponse update(Authentication authentication, String id, TableRequest request) {
        RestaurantTable table = findTable(id);
        Restaurant restaurant = restaurantService.findRestaurant(table.getRestaurantId());
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());

        table.setTableNumber(request.tableNumber());
        table.setCapacity(request.capacity());
        tableRepository.save(table);
        return toResponse(table);
    }

    public TableResponse updateStatus(Authentication authentication, String id, TableStatusUpdateRequest request) {
        RestaurantTable table = findTable(id);
        Restaurant restaurant = restaurantService.findRestaurant(table.getRestaurantId());
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());

        table.setStatus(request.status());
        tableRepository.save(table);
        return toResponse(table);
    }

    public void delete(Authentication authentication, String id) {
        RestaurantTable table = findTable(id);
        Restaurant restaurant = restaurantService.findRestaurant(table.getRestaurantId());
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());
        tableRepository.deleteById(id);
    }

    private RestaurantTable findTable(String id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
    }

    private TableResponse toResponse(RestaurantTable table) {
        return new TableResponse(
                table.getId(),
                table.getRestaurantId(),
                table.getTableNumber(),
                table.getCapacity(),
                table.getStatus()
        );
    }
}
