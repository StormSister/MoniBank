package com.monibank.mainframe.dashboard;

import com.monibank.mainframe.config.DailyCloseProperties;
import com.monibank.mainframe.dashboard.api.DailyCloseReportResponse;
import com.monibank.mainframe.interest.InterestPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyCloseOrchestrator {

    private final DailyCloseProperties properties;
    private final InterestPostingService interestPostingService;
    private final DailyStatisticsService dailyStatisticsService;
    private final DailyCloseReportService reportService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public DailyCloseReportResponse close(
            LocalDate businessDate,
            String requestedCurrency
    ) {
        if (businessDate == null) {
            throw new IllegalArgumentException(
                    "Business date is required."
            );
        }

        String currency = normalizeCurrency(requestedCurrency);

        if (!running.compareAndSet(false, true)) {
            throw new DailyCloseFailedException(
                    "Daily close is already in progress."
            );
        }

        try {
            log.info(
                    "DAILY CLOSE started for {} {}",
                    businessDate,
                    currency
            );

            interestPostingService.postDailyInterest(
                    businessDate,
                    currency,
                    properties.interestRateBasisPoints()
            );

            DailyCloseReportResponse report =
                    dailyStatisticsService.calculate(
                            businessDate,
                            currency
                    );

            reportService.cacheReport(report);
            return report;
        } finally {
            running.set(false);
            log.info(
                    "DAILY CLOSE finished for {}",
                    businessDate
            );
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private String normalizeCurrency(String currency) {
        String normalized = currency == null || currency.isBlank()
                ? properties.currency()
                : currency.toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must contain exactly three letters."
            );
        }

        return normalized;
    }
}
