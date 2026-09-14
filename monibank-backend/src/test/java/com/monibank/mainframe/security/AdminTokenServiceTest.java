package com.monibank.mainframe.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AdminTokenServiceTest {

    private static final String TEST_SECRET =
            "VGhpcy1pcy1hLXRlc3Qta2V5LXdpdGgtMzItYnl0ZXMhIQ==";

    @Test
    void issuedTokenIsSignedAndExpiresAfterOneHour() {
        AdminSecurityProperties properties = new AdminSecurityProperties(
                "admin",
                "password",
                TEST_SECRET,
                Duration.ofHours(1)
        );
        SecurityConfiguration configuration = new SecurityConfiguration();
        JwtEncoder encoder = configuration.jwtEncoder(properties);
        JwtDecoder decoder = configuration.jwtDecoder(properties);

        AdminTokenService.IssuedAdminToken issued =
                new AdminTokenService(encoder, properties).issue("admin");
        Jwt decoded = decoder.decode(issued.value());

        assertThat(decoded.getIssuer().toString()).isEqualTo("monibank");
        assertThat(decoded.getSubject()).isEqualTo("admin");
        assertThat(decoded.getClaimAsString("scope")).isEqualTo("admin");
        assertThat(issued.expiresInSeconds()).isEqualTo(3600L);
        assertThat(decoded.getExpiresAt()).isAfter(decoded.getIssuedAt());
    }
}
