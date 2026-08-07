package com.dinepilot.restaurant.service;

import com.dinepilot.common.exception.ResourceNotFoundException;
import com.dinepilot.restaurant.dto.CategoryRequest;
import com.dinepilot.restaurant.dto.CategoryResponse;
import com.dinepilot.restaurant.entity.Category;
import com.dinepilot.restaurant.entity.Restaurant;
import com.dinepilot.restaurant.repository.CategoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final RestaurantService restaurantService;
    private final OwnershipGuard ownershipGuard;

    public CategoryService(
            CategoryRepository categoryRepository,
            RestaurantService restaurantService,
            OwnershipGuard ownershipGuard
    ) {
        this.categoryRepository = categoryRepository;
        this.restaurantService = restaurantService;
        this.ownershipGuard = ownershipGuard;
    }

    public CategoryResponse create(Authentication authentication, CategoryRequest request) {
        Restaurant restaurant = restaurantService.findRestaurant(request.restaurantId());
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());

        Category category = new Category();
        applyRequest(category, request);
        categoryRepository.save(category);
        return toResponse(category);
    }

    public List<CategoryResponse> list(String restaurantId) {
        List<Category> categories = restaurantId == null
                ? categoryRepository.findAll()
                : categoryRepository.findByRestaurantId(restaurantId);
        return categories.stream().map(this::toResponse).toList();
    }

    public CategoryResponse update(Authentication authentication, String id, CategoryRequest request) {
        Category category = findCategory(id);
        Restaurant restaurant = restaurantService.findRestaurant(category.getRestaurantId());
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());

        applyRequest(category, request);
        categoryRepository.save(category);
        return toResponse(category);
    }

    public void delete(Authentication authentication, String id) {
        Category category = findCategory(id);
        Restaurant restaurant = restaurantService.findRestaurant(category.getRestaurantId());
        ownershipGuard.checkOwnerOrSuperAdmin(authentication, restaurant.getOwnerId());
        categoryRepository.deleteById(id);
    }

    private Category findCategory(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setRestaurantId(request.restaurantId());
        category.setName(request.name());
        category.setDescription(request.description());
        category.setDisplayOrder(request.displayOrder());
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getRestaurantId(),
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder()
        );
    }
}
