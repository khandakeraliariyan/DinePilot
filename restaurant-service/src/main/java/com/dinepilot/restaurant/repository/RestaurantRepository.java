package com.dinepilot.restaurant.repository;

import com.dinepilot.restaurant.entity.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RestaurantRepository extends MongoRepository<Restaurant, String> {

    List<Restaurant> findByOwnerId(String ownerId);
}
