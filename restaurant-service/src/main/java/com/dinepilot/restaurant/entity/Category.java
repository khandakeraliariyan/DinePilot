package com.dinepilot.restaurant.entity;

import com.dinepilot.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "categories")
public class Category extends BaseEntity {

    @Indexed
    private String restaurantId;

    private String name;

    private String description;

    private int displayOrder;
}
