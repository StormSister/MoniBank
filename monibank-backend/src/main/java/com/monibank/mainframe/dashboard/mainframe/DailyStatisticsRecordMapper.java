package com.monibank.mainframe.dashboard.mainframe;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class DailyStatisticsRecordMapper {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.BASIC_ISO_DATE;

    public String toRecord(
            LocalDate businessDate,
            String requestedCurrency
    ) {
        if (businessDate == null) {
            throw new IllegalArgumentException(
                    "Statistics business date is required."
            );
        }

        String currency = requestedCurrency == null
                ? ""
                : requestedCurrency.toUpperCase(Locale.ROOT);

        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Statistics currency must contain three letters."
            );
        }

        return businessDate.format(DATE_FORMAT) + currency;
    }
}
