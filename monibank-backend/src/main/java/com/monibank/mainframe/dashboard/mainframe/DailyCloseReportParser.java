package com.monibank.mainframe.dashboard.mainframe;

import com.monibank.mainframe.dashboard.api.DailyCloseReportResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DailyCloseReportParser {

    private static final String PREFIX = "MBS";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.BASIC_ISO_DATE;

    public DailyCloseReportResponse parse(
            String requestId,
            List<String> records
    ) {

        validateRequestId(requestId);

        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException(
                    "Daily close report cannot be empty."
            );
        }

        Map<String, String[]> recordsByType =
                collectRecords(records);

        String[] header =
                required(recordsByType, "H");
        String[] transactions =
                required(recordsByType, "T");
        String[] customers =
                required(recordsByType, "C");
        String[] end =
                required(recordsByType, "E");

        requireLength(header, 5, "H");
        requireLength(transactions, 11, "T");
        requireLength(customers, 8, "C");
        requireLength(end, 5, "E");

        LocalDate businessDate =
                parseDate(value(header, 2));
        String currency =
                value(header, 3);
        String state =
                value(header, 4);

        requireEqual(
                businessDate,
                parseDate(value(transactions, 2)),
                "transaction date"
        );
        requireEqual(
                businessDate,
                parseDate(value(customers, 2)),
                "customer date"
        );
        requireEqual(
                businessDate,
                parseDate(value(end, 2)),
                "end date"
        );
        requireEqual(
                currency,
                value(transactions, 3),
                "transaction currency"
        );
        requireEqual(
                currency,
                value(end, 3),
                "end currency"
        );

        if (!"ALL".equals(value(customers, 3))) {
            throw new IllegalArgumentException(
                    "MBS customer scope must be ALL."
            );
        }

        String resultCode =
                value(end, 4);

        if (!"CLOSED".equals(state)) {
            throw new IllegalArgumentException(
                    "Daily close report is not closed: "
                            + state
            );
        }

        if (!"OK".equals(resultCode)) {
            throw new IllegalArgumentException(
                    "Daily close report ended with code: "
                            + resultCode
            );
        }

        return new DailyCloseReportResponse(
                requestId,
                businessDate,
                currency,
                state,
                new DailyCloseReportResponse.TransactionSummary(
                        parseCount(transactions, 4),
                        parseCount(transactions, 5),
                        parseAmount(transactions, 6),
                        parseCount(transactions, 7),
                        parseAmount(transactions, 8),
                        parseCount(transactions, 9),
                        parseAmount(transactions, 10)
                ),
                new DailyCloseReportResponse.CustomerSummary(
                        parseCount(customers, 4),
                        parseCount(customers, 5),
                        parseCount(customers, 6),
                        parseCount(customers, 7)
                ),
                resultCode,
                Instant.now()
        );
    }

    private Map<String, String[]> collectRecords(
            List<String> records
    ) {

        Map<String, String[]> result =
                new HashMap<>();

        for (String rawRecord : records) {

            if (rawRecord == null || rawRecord.isBlank()) {
                continue;
            }

            String record =
                    rawRecord
                            .replace("\f", "")
                            .replace("\r", "")
                            .strip();

            if (!record.startsWith(PREFIX + ";")) {
                continue;
            }

            String[] parts =
                    record.split(";", -1);

            if (parts.length < 2) {
                throw new IllegalArgumentException(
                        "Invalid MBS record: " + record
                );
            }

            String type =
                    value(parts, 1);

            if (!type.matches("[HTCE]")) {
                throw new IllegalArgumentException(
                        "Unknown MBS record type: " + type
                );
            }

            if (result.putIfAbsent(type, parts) != null) {
                throw new IllegalArgumentException(
                        "Duplicate MBS record type: " + type
                );
            }
        }

        return result;
    }

    private String[] required(
            Map<String, String[]> records,
            String type
    ) {

        String[] record =
                records.get(type);

        if (record == null) {
            throw new IllegalArgumentException(
                    "Missing MBS record type: " + type
            );
        }

        return record;
    }

    private void requireLength(
            String[] record,
            int expected,
            String type
    ) {

        if (record.length != expected) {
            throw new IllegalArgumentException(
                    "MBS "
                            + type
                            + " record has "
                            + record.length
                            + " fields; expected "
                            + expected
                            + "."
            );
        }
    }

    private long parseCount(
            String[] record,
            int index
    ) {

        String value =
                value(record, index);

        try {

            long count =
                    Long.parseLong(value);

            if (count < 0) {
                throw new NumberFormatException(
                        "negative value"
                );
            }

            return count;

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Invalid MBS count: " + value,
                    exception
            );
        }
    }

    private BigDecimal parseAmount(
            String[] record,
            int index
    ) {

        String value =
                value(record, index);

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid MBS amount: " + value,
                    exception
            );
        }
    }

    private LocalDate parseDate(String value) {

        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Invalid MBS business date: " + value,
                    exception
            );
        }
    }

    private String value(
            String[] record,
            int index
    ) {

        if (index >= record.length) {
            return "";
        }

        return record[index].trim();
    }

    private void requireEqual(
            Object expected,
            Object actual,
            String description
    ) {

        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                    "MBS "
                            + description
                            + " "
                            + actual
                            + " does not match "
                            + expected
                            + "."
            );
        }
    }

    private void validateRequestId(String requestId) {

        if (requestId == null
                || !requestId.matches("R\\d{7}")) {
            throw new IllegalArgumentException(
                    "Request ID must match R followed by 7 digits."
            );
        }
    }
}
