package com.dinepilot.restaurant.controller;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.restaurant.dto.TableRequest;
import com.dinepilot.restaurant.dto.TableResponse;
import com.dinepilot.restaurant.dto.TableStatusUpdateRequest;
import com.dinepilot.restaurant.service.TableService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @PostMapping
    public ApiResponse<TableResponse> create(
            Authentication authentication,
            @Valid @RequestBody TableRequest request
    ) {
        return ApiResponse.success("Table created", tableService.create(authentication, request));
    }

    @GetMapping
    public ApiResponse<List<TableResponse>> list(@RequestParam(required = false) String restaurantId) {
        return ApiResponse.success(tableService.list(restaurantId));
    }

    @GetMapping("/{id}")
    public ApiResponse<TableResponse> get(@PathVariable String id) {
        return ApiResponse.success(tableService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<TableResponse> update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody TableRequest request
    ) {
        return ApiResponse.success("Table updated", tableService.update(authentication, id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<TableResponse> updateStatus(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody TableStatusUpdateRequest request
    ) {
        return ApiResponse.success("Status updated", tableService.updateStatus(authentication, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable String id) {
        tableService.delete(authentication, id);
        return ApiResponse.success("Table deleted", null);
    }
}
