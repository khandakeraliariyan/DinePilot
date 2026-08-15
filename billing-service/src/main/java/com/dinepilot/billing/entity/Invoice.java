package com.dinepilot.billing.entity;

import com.dinepilot.common.entity.BaseEntity;
import com.dinepilot.billing.enums.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Getter @Setter
@Document(collection = "invoices")
public class Invoice extends BaseEntity {
    @Indexed private String orderId;
    @Indexed private String userId;
    @Indexed private String restaurantId;
    @Indexed private InvoiceStatus status;
    private BigDecimal amount;
    private String receiptNumber;
}
