package com.dinepilot.billing.entity;

import com.dinepilot.common.entity.BaseEntity;
import com.dinepilot.billing.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter
@Document(collection = "payments")
public class Payment extends BaseEntity {
    @Indexed private String invoiceId;
    @Indexed private PaymentStatus status;
    private BigDecimal amount;
    private Instant processedAt;
    private String providerRef;
}
