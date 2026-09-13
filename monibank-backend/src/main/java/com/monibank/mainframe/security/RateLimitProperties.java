package com.monibank.mainframe.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "monibank.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        String trustedClientIpHeader,
        int maxTrackedClients,
        Duration idleTtl,
        Policy read,
        Policy write
) {

    public RateLimitProperties {
        if (maxTrackedClients < 1) {
            throw new IllegalArgumentException(
                    "Rate-limit max-tracked-clients must be positive."
            );
        }
        if (idleTtl == null || idleTtl.isNegative() || idleTtl.isZero()) {
            throw new IllegalArgumentException(
                    "Rate-limit idle-ttl must be positive."
            );
        }
        requirePolicy(read, "read");
        requirePolicy(write, "write");
    }

    private static void requirePolicy(Policy policy, String name) {
        if (policy == null) {
            throw new IllegalArgumentException(
                    "Missing rate-limit policy: " + name + "."
            );
        }
    }

    public record Policy(
            int capacity,
            int refillTokens,
            Duration refillPeriod
    ) {

        public Policy {
            if (capacity < 1 || refillTokens < 1) {
                throw new IllegalArgumentException(
                        "Rate-limit capacity and refill-tokens must be positive."
                );
            }
            if (refillPeriod == null
                    || refillPeriod.isNegative()
                    || refillPeriod.isZero()) {
                throw new IllegalArgumentException(
                        "Rate-limit refill-period must be positive."
                );
            }
        }
    }
}
