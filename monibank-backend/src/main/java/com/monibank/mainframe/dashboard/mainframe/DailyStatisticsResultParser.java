package com.monibank.mainframe.dashboard.mainframe;

import com.monibank.mainframe.dashboard.api.DailyCloseReportResponse;
import com.monibank.mainframe.model.MainframeDataRecord;
import com.monibank.mainframe.model.MainframeResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DailyStatisticsResultParser {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.BASIC_ISO_DATE;

    private static final int PAYLOAD_LENGTH = 119;

    public DailyCloseReportResponse parse(MainframeResult result) {
        if (result == null) {
            throw new IllegalArgumentException(
                    "Daily statistics result cannot be null."
            );
        }

        if (!"OK".equals(result.header().code())) {
            throw new IllegalStateException(
                    "Daily statistics operation ended with code: "
                            + result.header().code()
            );
        }

        if (!"C".equals(result.header().status())) {
            throw new IllegalStateException(
                    "Daily statistics operation returned status: "
                            + result.header().status()
            );
        }

        MainframeDataRecord transactions =
                singleRecord(result.data(), "DAYTXN");
        MainframeDataRecord customers =
                singleRecord(result.data(), "DAYCUST");

        String txn = payload(transactions);
        String cust = payload(customers);

        LocalDate businessDate = parseDate(field(txn, 0, 8));
        String currency = field(txn, 8, 11);

        requireEqual(
                businessDate,
                parseDate(field(cust, 0, 8)),
                "business date"
        );
        requireEqual("ALL", field(cust, 8, 11), "customer scope");

        String requestId = result.header().entityId();
        if (requestId == null || !requestId.matches("R\\d{7}")) {
            throw new IllegalStateException(
                    "Daily statistics returned invalid request ID: "
                            + requestId
            );
        }

        return new DailyCloseReportResponse(
                requestId,
                businessDate,
                currency,
                "CLOSED",
                new DailyCloseReportResponse.TransactionSummary(
                        count(txn, 11, 20),
                        count(txn, 20, 29),
                        amount(txn, 29, 46),
                        count(txn, 46, 55),
                        amount(txn, 55, 72),
                        count(txn, 72, 81),
                        amount(txn, 81, 98)
                ),
                new DailyCloseReportResponse.CustomerSummary(
                        count(cust, 11, 20),
                        count(cust, 20, 29),
                        count(cust, 29, 38),
                        count(cust, 38, 47)
                ),
                result.header().code(),
                Instant.now()
        );
    }

    private MainframeDataRecord singleRecord(
            List<MainframeDataRecord> records,
            String entityType
    ) {
        List<MainframeDataRecord> matching = records.stream()
                .filter(record -> entityType.equals(record.entityType()))
                .toList();

        if (matching.size() != 1) {
            throw new IllegalStateException(
                    "Daily statistics returned " + matching.size() + " "
                            + entityType + " records; expected 1."
            );
        }

        return matching.getFirst();
    }

    private String payload(MainframeDataRecord record) {
        String payload = record.payload();
        if (payload == null || payload.length() != PAYLOAD_LENGTH) {
            throw new IllegalStateException(
                    "Daily statistics " + record.entityType()
                            + " payload must contain 119 characters."
            );
        }
        return payload;
    }

    private String field(String payload, int start, int end) {
        return payload.substring(start, end).trim();
    }

    private long count(String payload, int start, int end) {
        String value = field(payload, start, end);
        if (!value.matches("\\d+")) {
            throw new IllegalStateException(
                    "Invalid daily statistics count: " + value
            );
        }
        return Long.parseLong(value);
    }

    private BigDecimal amount(String payload, int start, int end) {
        String value = field(payload, start, end);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Invalid daily statistics amount: " + value,
                    exception
            );
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Invalid daily statistics date: " + value,
                    exception
            );
        }
    }

    private void requireEqual(
            Object expected,
            Object actual,
            String description
    ) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    "Unexpected daily statistics " + description + ": "
                            + actual + ", expected " + expected + "."
            );
        }
    }
}
