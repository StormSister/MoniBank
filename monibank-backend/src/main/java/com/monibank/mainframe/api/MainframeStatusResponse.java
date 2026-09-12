package com.monibank.mainframe.api;

import com.monibank.mainframe.hercules.HerculesRuntimeMetrics;

import java.time.Instant;
import java.util.List;

public record MainframeStatusResponse(
        String provider,
        String system,
        String systemId,
        String status,
        ConnectionStatus connections,
        HerculesRuntimeMetrics runtime,
        boolean metricsStale,
        Instant checkedAt
) {

    public record ConnectionStatus(
            String reader,
            String resultPrinter,
            String terminal,
            int configuredTerminals,
            int readyTerminals,
            int busyTerminals,
            int recoveringTerminals,
            int queuedRequests,
            List<TerminalStatus> terminals
    ) {
    }

    public record TerminalStatus(
            String id,
            String username,
            String state,
            String currentRequestId,
            long recoveryCount,
            Instant readySince,
            Instant lastFailureAt,
            String lastError
    ) {
    }
}
