package com.monibank.mainframe.statement.api;

import com.monibank.mainframe.transaction.api.TransactionResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AccountStatementResponse(
        Account account,
        Period period,
        Summary summary,
        List<TransactionResponse> transactions,
        Instant generatedAt
) {

    public record Account(
            String accountId,
            String customerId,
            String iban,
            String type,
            String currency,
            String status
    ) {
    }

    public record Period(
            LocalDate from,
            LocalDate to
    ) {
    }

    public record Summary(
            BigDecimal openingBalance,
            BigDecimal totalCredits,
            long creditCount,
            BigDecimal totalDebits,
            long debitCount,
            BigDecimal closingBalance,
            long transactionCount
    ) {
    }
}
