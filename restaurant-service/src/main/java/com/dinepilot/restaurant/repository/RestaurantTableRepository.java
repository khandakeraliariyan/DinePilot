package com.dinepilot.restaurant.repository;

import com.dinepilot.restaurant.entity.RestaurantTable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RestaurantTableRepository extends MongoRepository<RestaurantTable, String> {

    List<RestaurantTable> findByRestaurantId(String restaurantId);
}
