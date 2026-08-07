package com.dinepilot.restaurant.entity;

import com.dinepilot.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document(collection = "restaurants")
public class Restaurant extends BaseEntity {

    @Indexed
    private String ownerId;

    private String name;

    private String description;

    private Address address = new Address();

    private List<OpeningHours> openingHours = new ArrayList<>();

    private boolean active = true;
}
