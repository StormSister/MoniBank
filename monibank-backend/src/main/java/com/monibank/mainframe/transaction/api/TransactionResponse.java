package com.monibank.mainframe.transaction.api;

import java.math.BigDecimal;

public record TransactionResponse(
        String transactionId,
        String accountId,
        String direction,
        String type,
        String currency,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String detail,
        String sourceId,
        String createdAt,
        String status
) {
}
