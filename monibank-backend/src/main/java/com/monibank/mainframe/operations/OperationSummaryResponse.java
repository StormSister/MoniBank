package com.monibank.mainframe.operations;

import java.time.Instant;
import java.util.List;

public record OperationSummaryResponse(
        Instant from,
        Instant to,
        long totalCount,
        long successCount,
        long businessErrorCount,
        long technicalErrorCount,
        double successRatePercent,
        long averageQueueMs,
        long averageDurationMs,
        List<ExecutorSummary> executors,
        List<OperationBreakdown> operations
) {

    public record ExecutorSummary(
            String executorId,
            long totalCount,
            long successCount,
            long businessErrorCount,
            long technicalErrorCount,
            long averageDurationMs
    ) {
    }

    public record OperationBreakdown(
            String operation,
            long totalCount,
            long successCount,
            long businessErrorCount,
            long technicalErrorCount
    ) {
    }
}
