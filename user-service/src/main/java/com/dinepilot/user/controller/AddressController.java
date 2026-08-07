package com.dinepilot.user.controller;

import com.dinepilot.common.dto.ApiResponse;
import com.dinepilot.user.dto.AddressRequest;
import com.dinepilot.user.dto.AddressResponse;
import com.dinepilot.user.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list(Authentication authentication) {
        return ApiResponse.success(addressService.list(authentication.getName()));
    }

    @PostMapping
    public ApiResponse<AddressResponse> create(
            Authentication authentication,
            @Valid @RequestBody AddressRequest request
    ) {
        return ApiResponse.success("Address created", addressService.create(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody AddressRequest request
    ) {
        return ApiResponse.success("Address updated", addressService.update(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable String id) {
        addressService.delete(authentication.getName(), id);
        return ApiResponse.success("Address deleted", null);
    }
}
