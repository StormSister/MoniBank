package com.monibank.mainframe.interest.mainframe;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class InterestRecordMapper {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.BASIC_ISO_DATE;

    public String toRecord(
            LocalDate businessDate,
            String requestedCurrency,
            int rateBasisPoints
    ) {
        if (businessDate == null) {
            throw new IllegalArgumentException(
                    "Interest business date is required."
            );
        }

        String currency = requestedCurrency == null
                ? ""
                : requestedCurrency.toUpperCase(Locale.ROOT);

        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Interest currency must contain three letters."
            );
        }

        if (rateBasisPoints < 1 || rateBasisPoints > 99999) {
            throw new IllegalArgumentException(
                    "Interest rate must be between 1 and 99999 basis points."
            );
        }

        return businessDate.format(DATE_FORMAT)
                + currency
                + String.format(Locale.ROOT, "%05d", rateBasisPoints);
    }
}
