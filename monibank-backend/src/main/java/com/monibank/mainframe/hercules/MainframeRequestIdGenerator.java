package com.monibank.mainframe.hercules;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class MainframeRequestIdGenerator {

    private static final long REQUEST_ID_LIMIT =
            10_000_000L;

    private final AtomicLong sequence =
            new AtomicLong(
                    System.currentTimeMillis()
                            % REQUEST_ID_LIMIT
            );

    public String next() {

        long value =
                sequence.updateAndGet(
                        current ->
                                (current + 1)
                                        % REQUEST_ID_LIMIT
                );

        return "R%07d".formatted(value);
    }
}
