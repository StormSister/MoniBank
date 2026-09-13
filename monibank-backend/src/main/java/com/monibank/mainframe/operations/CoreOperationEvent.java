package com.monibank.mainframe.operations;

import java.time.Instant;

public record CoreOperationEvent(
        String requestId,
        Core core,
        String provider,
        String systemId,
        String channel,
        String operation,
        ExecutorType executorType,
        String executorId,
        String username,
        Instant acceptedAt,
        Instant assignedAt,
        Instant completedAt,
        long queueMs,
        long durationMs,
        Status status,
        String resultCode,
        String errorType,
        Boolean retryable
) {

    public enum Core {
        LEGACY,
        MODERN
    }

    public enum ExecutorType {
        TERMINAL_WORKER,
        CICS_REGION,
        SERVICE
    }

    public enum Status {
        SUCCESS,
        BUSINESS_ERROR,
        TECHNICAL_ERROR
    }
}
