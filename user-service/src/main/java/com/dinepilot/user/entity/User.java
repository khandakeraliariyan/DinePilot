package com.dinepilot.user.entity;

import com.dinepilot.common.entity.BaseEntity;
import com.dinepilot.common.enums.Role;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "users")
public class User extends BaseEntity {

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String fullName;

    private String phone;

    private Role role = Role.CUSTOMER;

    private boolean enabled = true;
}
