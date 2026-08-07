package com.dinepilot.order.entity;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class CartItem {
    private String foodId;
    private String restaurantId;
    private String foodName;
    private BigDecimal unitPrice;
    private int quantity;
}
