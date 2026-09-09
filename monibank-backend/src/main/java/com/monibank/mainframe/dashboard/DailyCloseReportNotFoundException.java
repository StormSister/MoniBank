package com.monibank.mainframe.dashboard;

import java.time.LocalDate;

public class DailyCloseReportNotFoundException
        extends DailyCloseReportUnavailableException {

    public DailyCloseReportNotFoundException(
            LocalDate businessDate,
            String currency,
            Throwable cause
    ) {
        super(
                "No closed-day report exists for "
                        + businessDate
                        + " "
                        + currency
                        + ".",
                cause
        );
    }
}
