package com.dinepilot.restaurant.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Address {

    private String line1;

    private String city;

    private String state;

    private String postalCode;

    private String country;
}
