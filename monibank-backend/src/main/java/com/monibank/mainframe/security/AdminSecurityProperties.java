package com.monibank.mainframe.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Base64;

@ConfigurationProperties(prefix = "monibank.security.admin")
public record AdminSecurityProperties(
        String username,
        String password,
        String jwtSecret,
        Duration tokenTtl
) {

    private static final int MINIMUM_SECRET_BYTES = 32;

    public AdminSecurityProperties {
        requireText(username, "MONIBANK_ADMIN_USERNAME");
        requireText(password, "MONIBANK_ADMIN_PASSWORD");
        requireText(jwtSecret, "MONIBANK_JWT_SECRET");

        if (tokenTtl == null || tokenTtl.isNegative()
                || tokenTtl.isZero()) {
            throw new IllegalArgumentException(
                    "Admin token TTL must be positive."
            );
        }

        byte[] decodedSecret;
        try {
            decodedSecret = Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "MONIBANK_JWT_SECRET must be Base64 encoded.",
                    exception
            );
        }
        if (decodedSecret.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "MONIBANK_JWT_SECRET must contain at least 32 random bytes."
            );
        }
    }

    public byte[] decodedJwtSecret() {
        return Base64.getDecoder().decode(jwtSecret);
    }

    private static void requireText(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing required environment variable: "
                            + variableName + "."
            );
        }
    }
}
