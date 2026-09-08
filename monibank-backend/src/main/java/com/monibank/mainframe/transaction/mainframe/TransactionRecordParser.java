package com.monibank.mainframe.transaction.mainframe;

import com.monibank.mainframe.transaction.api.TransactionResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionRecordParser {

    private static final int RECORD_LENGTH = 119;

    public TransactionResponse parse(
            String record
    ) {

        requireLength(record);

        return new TransactionResponse(
                field(record, 14, 27),
                field(record, 1, 14),
                field(record, 27, 28),
                field(record, 28, 30),
                field(record, 30, 33),
                amount(record, 33, 50),
                amount(record, 50, 67),
                field(record, 67, 87),
                field(record, 87, 100),
                field(record, 100, 114),
                field(record, 0, 1)
        );
    }

    private BigDecimal amount(
            String record,
            int from,
            int to
    ) {

        String value = field(record, from, to);

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid transaction amount: " + value,
                    exception
            );
        }
    }

    private String field(
            String record,
            int from,
            int to
    ) {
        return record.substring(from, to).trim();
    }

    private void requireLength(
            String record
    ) {

        if (record == null
                || record.length() != RECORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid transaction record length: "
                            + (record == null
                            ? 0
                            : record.length())
            );
        }
    }
}
