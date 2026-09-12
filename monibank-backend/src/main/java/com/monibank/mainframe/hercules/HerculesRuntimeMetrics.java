package com.monibank.mainframe.hercules;

import java.time.Instant;

public record HerculesRuntimeMetrics(
        String containerState,
        double cpuPercent,
        String memoryUsed,
        String memoryLimit,
        double memoryPercent,
        long uptimeSeconds,
        Instant sampledAt
) {
}
