package com.monibank.mainframe.account.mainframe;

import com.monibank.mainframe.account.api.AccountResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountRecordParser {

    private static final int RECORD_LENGTH = 119;

    public AccountResponse parse(
            String record
    ) {

        requireLength(record);

        return new AccountResponse(
                field(record, 1, 14),
                field(record, 14, 27),
                field(record, 27, 61),
                field(record, 61, 63),
                field(record, 63, 66),
                amount(record, 66, 83),
                amount(record, 83, 100),
                amount(record, 100, 117),
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
                    "Invalid account amount: " + value,
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
                    "Invalid account record length: "
                            + (record == null
                            ? 0
                            : record.length())
            );
        }
    }
}
