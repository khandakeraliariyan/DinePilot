package com.dinepilot.restaurant.repository;

import com.dinepilot.restaurant.entity.Food;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FoodRepository extends MongoRepository<Food, String> {
}
