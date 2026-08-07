package com.dinepilot.restaurant.controller;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.restaurant.dto.CategoryRequest;
import com.dinepilot.restaurant.dto.CategoryResponse;
import com.dinepilot.restaurant.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(
            Authentication authentication,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ApiResponse.success("Category created", categoryService.create(authentication, request));
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list(@RequestParam(required = false) String restaurantId) {
        return ApiResponse.success(categoryService.list(restaurantId));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ApiResponse.success("Category updated", categoryService.update(authentication, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable String id) {
        categoryService.delete(authentication, id);
        return ApiResponse.success("Category deleted", null);
    }
}
