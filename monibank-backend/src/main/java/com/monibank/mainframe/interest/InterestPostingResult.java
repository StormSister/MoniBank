package com.monibank.mainframe.interest;

import java.time.LocalDate;

public record InterestPostingResult(
        LocalDate businessDate,
        String currency,
        int annualRateBasisPoints,
        long postedAccountCount,
        String status
) {
}
