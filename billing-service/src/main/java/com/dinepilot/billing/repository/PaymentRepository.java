package com.dinepilot.billing.repository;

import com.dinepilot.billing.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByInvoiceIdOrderByProcessedAtDesc(String invoiceId);
}
