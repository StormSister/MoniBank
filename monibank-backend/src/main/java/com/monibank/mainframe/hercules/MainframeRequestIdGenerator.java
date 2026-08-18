package com.monibank.mainframe.hercules;

import org.springframework.stereotype.Component;

@Component
public class MainframeRequestIdGenerator {

    public String next() {

        long value =
                System.currentTimeMillis() % 10_000_000;

        return "R%07d".formatted(value);
    }
}