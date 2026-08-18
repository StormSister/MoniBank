package com.monibank.mainframe.hercules;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class JobNameGenerator {

    private final AtomicInteger sequence = new AtomicInteger(1);

    public String next() {
        return "MBT%05d".formatted(sequence.getAndIncrement());
    }
}