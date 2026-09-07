package com.monibank.mainframe.card.mainframe;

import com.monibank.mainframe.card.api.CardResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CardRecordParser {

    private static final int RECORD_LENGTH = 119;

    public CardResponse parse(
            String record
    ) {

        requireLength(record);

        return new CardResponse(
                field(record, 1, 14),
                field(record, 14, 27),
                field(record, 27, 40),
                field(record, 40, 56),
                field(record, 56, 57),
                field(record, 57, 59),
                field(record, 59, 65),
                amount(record, 65, 82),
                amount(record, 82, 99),
                field(record, 99, 107),
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
                    "Invalid card amount: " + value,
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
                    "Invalid card record length: "
                            + (record == null
                            ? 0
                            : record.length())
            );
        }
    }
}
