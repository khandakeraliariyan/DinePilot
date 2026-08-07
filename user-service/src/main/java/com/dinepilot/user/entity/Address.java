package com.dinepilot.user.entity;

import com.dinepilot.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "addresses")
public class Address extends BaseEntity {

    @Indexed
    private String userId;

    private String label;

    private String line1;

    private String line2;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private boolean defaultAddress;
}
