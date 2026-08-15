package com.dinepilot.billing.repository;

import com.dinepilot.billing.entity.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    Optional<Invoice> findByOrderId(String orderId);
    List<Invoice> findByUserIdOrderByCreatedAtDesc(String userId);
}
