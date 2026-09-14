package com.monibank.mainframe.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminSecurityPropertiesTest {

    @Test
    void rejectsJwtSecretShorterThan256Bits() {
        assertThatThrownBy(() -> new AdminSecurityProperties(
                "admin",
                "password",
                "c2hvcnQ=",
                Duration.ofHours(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 random bytes");
    }
}
