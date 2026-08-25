package com.monibank.mainframe.hercules;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class MainframeIdGenerator {

    private static final long LIMIT =
            1_000_000_000_000L;

    private final SecureRandom random =
            new SecureRandom();

    public String nextNumber() {

        long value =
                random.nextLong(LIMIT);

        return String.format(
                "%012d",
                value
        );
    }
}