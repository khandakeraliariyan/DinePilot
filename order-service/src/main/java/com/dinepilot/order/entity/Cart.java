package com.dinepilot.order.entity;

import com.dinepilot.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@Document(collection = "carts")
public class Cart extends BaseEntity {
    @Indexed(unique = true) private String userId;
    private String restaurantId;
    private List<CartItem> items = new ArrayList<>();
}
