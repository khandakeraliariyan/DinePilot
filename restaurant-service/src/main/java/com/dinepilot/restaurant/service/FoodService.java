package com.dinepilot.restaurant.service;

import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.restaurant.dto.FoodAvailabilityRequest;
import com.dinepilot.restaurant.dto.FoodRequest;
import com.dinepilot.restaurant.dto.FoodResponse;
import com.dinepilot.restaurant.entity.Food;
import com.dinepilot.restaurant.entity.Restaurant;
import com.dinepilot.restaurant.repository.FoodRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final MongoTemplate mongoTemplate;
    private final RestaurantService restaurantService;
    private final OwnershipGuard ownershipGuard;

    public FoodService(
            FoodRepository foodRepository,
            MongoTemplate mongoTemplate,
            RestaurantService restaurantService,
            OwnershipGuard ownershipGuard
    ) {
        this.foodRepository = foodRepository;
        this.mongoTemplate = mongoTemplate;
        this.restaurantService = restaurantService;
        this.ownershipGuard = ownershipGuard;
    }

    public FoodResponse create(Authentication authentication, FoodRequest request) {
        Restaurant restaurant = restaurantService.findRestaurant(request.restaurantId());
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());

        Food food = new Food();
        applyRequest(food, request);
        foodRepository.save(food);
        return toResponse(food);
    }

    public FoodResponse get(String id) {
        return toResponse(findFood(id));
    }

    public List<FoodResponse> search(String restaurantId, String categoryId, Boolean available, String q) {
        Query query = new Query();
        List<Criteria> criteria = new java.util.ArrayList<>();

        if (restaurantId != null) {
            criteria.add(Criteria.where("restaurantId").is(restaurantId));
        }
        if (categoryId != null) {
            criteria.add(Criteria.where("categoryId").is(categoryId));
        }
        if (available != null) {
            criteria.add(Criteria.where("available").is(available));
        }
        if (q != null && !q.isBlank()) {
            criteria.add(Criteria.where("name").regex(q, "i"));
        }

        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, Food.class).stream().map(this::toResponse).toList();
    }

    public FoodResponse update(Authentication authentication, String id, FoodRequest request) {
        Food food = findFood(id);
        Restaurant restaurant = restaurantService.findRestaurant(food.getRestaurantId());
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());

        applyRequest(food, request);
        foodRepository.save(food);
        return toResponse(food);
    }

    public FoodResponse updateAvailability(Authentication authentication, String id, FoodAvailabilityRequest request) {
        Food food = findFood(id);
        Restaurant restaurant = restaurantService.findRestaurant(food.getRestaurantId());
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());

        food.setAvailable(request.available());
        foodRepository.save(food);
        return toResponse(food);
    }

    public void delete(Authentication authentication, String id) {
        Food food = findFood(id);
        Restaurant restaurant = restaurantService.findRestaurant(food.getRestaurantId());
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());
        foodRepository.deleteById(id);
    }

    private Food findFood(String id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food not found"));
    }

    private void applyRequest(Food food, FoodRequest request) {
        food.setRestaurantId(request.restaurantId());
        food.setCategoryId(request.categoryId());
        food.setName(request.name());
        food.setDescription(request.description());
        food.setPrice(request.price());
        food.setAvailable(request.available());
    }

    private FoodResponse toResponse(Food food) {
        return new FoodResponse(
                food.getId(),
                food.getRestaurantId(),
                food.getCategoryId(),
                food.getName(),
                food.getDescription(),
                food.getPrice(),
                food.isAvailable()
        );
    }
}
