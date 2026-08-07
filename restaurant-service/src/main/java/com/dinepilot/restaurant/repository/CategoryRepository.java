package com.dinepilot.restaurant.repository;

import com.dinepilot.restaurant.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CategoryRepository extends MongoRepository<Category, String> {

    List<Category> findByRestaurantId(String restaurantId);
}
