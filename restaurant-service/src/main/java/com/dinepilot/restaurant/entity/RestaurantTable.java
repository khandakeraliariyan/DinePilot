package com.dinepilot.restaurant.entity;

import com.dinepilot.common.entity.BaseEntity;
import com.dinepilot.restaurant.enums.TableStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "tables")
public class RestaurantTable extends BaseEntity {

    @Indexed
    private String restaurantId;

    private String tableNumber;

    private int capacity;

    private TableStatus status = TableStatus.AVAILABLE;
}
