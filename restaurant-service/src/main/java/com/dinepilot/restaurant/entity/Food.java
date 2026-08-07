package com.dinepilot.restaurant.entity;

import com.dinepilot.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Getter
@Setter
@Document(collection = "foods")
public class Food extends BaseEntity {

    @Indexed
    private String restaurantId;

    @Indexed
    private String categoryId;

    private String name;

    private String description;

    private BigDecimal price;

    private boolean available = true;
}
