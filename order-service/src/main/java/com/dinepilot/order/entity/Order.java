package com.dinepilot.order.entity;

import com.dinepilot.common.entity.BaseEntity;
import com.dinepilot.order.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Document(collection = "orders")
public class Order extends BaseEntity {
    @Indexed private String userId;
    @Indexed private String restaurantId;
    @Indexed private OrderStatus status;
    @Indexed private boolean paid;
    private List<OrderItem> items = new ArrayList<>();
    private BigDecimal total;
}
