package com.dinepilot.order.entity;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class OrderItem {
    private String foodId;
    private String foodName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
}
