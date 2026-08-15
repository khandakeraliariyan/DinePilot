package com.dinepilot.billing.controller;

import com.dinepilot.billing.entity.Invoice;
import com.dinepilot.billing.entity.Payment;
import com.dinepilot.billing.service.BillingService;
import com.dinepilot.common.dto.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BillingController {
    private final BillingService billingService;

    public BillingController(BillingService billingService) { this.billingService = billingService; }

    @PostMapping("/invoices/orders/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','RESTAURANT_ADMIN','SUPER_ADMIN')")
    public ApiResponse<Invoice> generate(@PathVariable String orderId, Authentication authentication) {
        boolean elevated = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        return ApiResponse.success("Invoice generated", billingService.generateInvoice(orderId, authentication.getName(), elevated));
    }

    @GetMapping("/invoices/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<Invoice>> mine(Authentication authentication) {
        return ApiResponse.success(billingService.listMyInvoices(authentication.getName()));
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','RESTAURANT_ADMIN','SUPER_ADMIN')")
    public ApiResponse<Invoice> get(@PathVariable String id, Authentication authentication) {
        boolean elevated = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        return ApiResponse.success(billingService.getInvoice(id, authentication.getName(), elevated));
    }

    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize("hasAnyRole('CUSTOMER','RESTAURANT_ADMIN','SUPER_ADMIN')")
    public ApiResponse<Payment> pay(@PathVariable String id, Authentication authentication) {
        boolean elevated = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        return ApiResponse.success("Payment completed", billingService.payInvoice(id, authentication.getName(), elevated));
    }
}
