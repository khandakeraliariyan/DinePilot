package com.dinepilot.billing.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSecretCompatibilityTest {

    @Test
    void userServiceAndBillingServiceUseTheSameDefaultJwtSecret() throws IOException {
        String userServiceSecret = extractDefaultJwtSecret(
                Path.of("../user-service/src/main/resources/application.yml"));
        String billingServiceSecret = extractDefaultJwtSecret(Path.of("src/main/resources/application.yml"));

        assertThat(billingServiceSecret)
                .as("All services must use the same JWT secret fallback when JWT_SECRET is not set")
                .isEqualTo(userServiceSecret);
    }

    private String extractDefaultJwtSecret(Path path) throws IOException {
        String content = Files.readString(path);
        int index = content.indexOf("jwt:");
        assertThat(index).isNotEqualTo(-1);

        String secretLine = content.lines()
                .filter(line -> line.contains("secret:"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("JWT secret property not found in " + path));

        String prefix = "${JWT_SECRET:";
        int start = secretLine.indexOf(prefix);
        assertThat(start).isNotEqualTo(-1);

        int end = secretLine.indexOf('}', start + prefix.length());
        assertThat(end).isNotEqualTo(-1);

        return secretLine.substring(start + prefix.length(), end).trim();
    }
}
