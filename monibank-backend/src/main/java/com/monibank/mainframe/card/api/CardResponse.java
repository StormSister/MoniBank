package com.monibank.mainframe.card.api;

import java.math.BigDecimal;

public record CardResponse(
        String cardId,
        String accountId,
        String customerId,
        String cardNumber,
        String type,
        String network,
        String expiry,
        BigDecimal dailyLimit,
        BigDecimal dailySpent,
        String spentDate,
        String status
) {
}
