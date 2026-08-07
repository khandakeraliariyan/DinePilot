package com.dinepilot.user.entity;

import com.dinepilot.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document(collection = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @Indexed
    private String userId;

    @Indexed(unique = true)
    private String tokenHash;

    private Instant expiresAt;

    private boolean revoked;
}
