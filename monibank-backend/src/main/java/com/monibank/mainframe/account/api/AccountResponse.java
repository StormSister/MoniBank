package com.monibank.mainframe.account.api;

import java.math.BigDecimal;

public record AccountResponse(
        String accountId,
        String customerId,
        String iban,
        String type,
        String currency,
        BigDecimal balance,
        BigDecimal overdraftLimit,
        BigDecimal blockedAmount,
        String status
) {
}
