package com.monibank.mainframe.dashboard.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DailyCloseReportResponse(
        String requestId,
        LocalDate businessDate,
        String currency,
        String state,
        TransactionSummary transactions,
        CustomerSummary customers,
        String resultCode,
        Instant loadedAt
) {

    public record TransactionSummary(
            long operationCount,
            long depositCount,
            BigDecimal depositAmount,
            long withdrawalCount,
            BigDecimal withdrawalAmount,
            long interestCount,
            BigDecimal interestAmount
    ) {
    }

    public record CustomerSummary(
            long totalCount,
            long activeCount,
            long inactiveCount,
            long newCount
    ) {
    }
}
