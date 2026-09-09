package com.monibank.mainframe.dashboard;

import com.monibank.mainframe.dashboard.api.DailyCloseReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DailyCloseReportService {

    private static final int MAX_CACHED_REPORTS = 8;

    private final DailyStatisticsService dailyStatisticsService;

    private final Map<ReportKey, DailyCloseReportResponse> cache =
            new LinkedHashMap<>(16, 0.75f, true);

    public synchronized DailyCloseReportResponse getReport(
            LocalDate businessDate,
            String currency
    ) {

        if (businessDate == null) {
            throw new IllegalArgumentException(
                    "Business date is required."
            );
        }

        String normalizedCurrency =
                normalizeCurrency(currency);

        ReportKey key =
                new ReportKey(
                        businessDate,
                        normalizedCurrency
                );

        DailyCloseReportResponse cached =
                cache.get(key);

        if (cached != null) {
            return cached;
        }

        DailyCloseReportResponse loaded =
                dailyStatisticsService.load(
                        businessDate,
                        normalizedCurrency
                );

        cache.put(key, loaded);
        evictOldestEntryIfNeeded();

        return loaded;
    }

    public synchronized void cacheReport(
            DailyCloseReportResponse report
    ) {
        if (report == null) {
            throw new IllegalArgumentException(
                    "Daily close report is required."
            );
        }

        ReportKey key = new ReportKey(
                report.businessDate(),
                normalizeCurrency(report.currency())
        );

        cache.put(key, report);
        evictOldestEntryIfNeeded();
    }

    public synchronized boolean isCached(
            LocalDate businessDate,
            String currency
    ) {
        return cache.containsKey(
                new ReportKey(
                        businessDate,
                        normalizeCurrency(currency)
                )
        );
    }

    private String normalizeCurrency(String currency) {

        if (currency == null
                || !currency.matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must contain exactly three letters."
            );
        }

        return currency.toUpperCase(Locale.ROOT);
    }

    private void evictOldestEntryIfNeeded() {

        if (cache.size() <= MAX_CACHED_REPORTS) {
            return;
        }

        ReportKey oldest =
                cache.keySet()
                        .iterator()
                        .next();

        cache.remove(oldest);
    }

    private record ReportKey(
            LocalDate businessDate,
            String currency
    ) {
    }
}
