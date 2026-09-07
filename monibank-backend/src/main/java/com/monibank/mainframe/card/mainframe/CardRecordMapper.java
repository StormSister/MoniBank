package com.monibank.mainframe.card.mainframe;

import com.monibank.mainframe.card.api.CreateCardRequest;
import com.monibank.mainframe.mapping.MainframeRecordMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
public class CardRecordMapper
        implements MainframeRecordMapper<CreateCardRequest> {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    public String toRecord(
            CreateCardRequest request
    ) {

        Objects.requireNonNull(
                request,
                "Create card request cannot be null."
        );

        validateAccountId(request.accountId());

        if (request.expiry() == null) {
            throw new IllegalArgumentException(
                    "Card expiry cannot be null."
            );
        }

        String expiry =
                request.expiry()
                        .format(EXPIRY_FORMAT);

        String createdAt =
                LocalDateTime.now()
                        .format(TIMESTAMP_FORMAT);

        String record =
                request.accountId()
                        + amount(request.dailyLimit())
                        + expiry
                        + createdAt;

        requireLength(
                record,
                48,
                "Card data"
        );

        return record;
    }

    public String toStatusUpdateRecord(
            String cardId,
            String status
    ) {

        validateCardId(cardId);
        validateStatus(status);

        String updatedAt =
                LocalDateTime.now()
                        .format(TIMESTAMP_FORMAT);

        String record =
                cardId
                        + status
                        + updatedAt;

        requireLength(
                record,
                28,
                "Card status update record"
        );

        return record;
    }

    private String amount(
            BigDecimal value
    ) {

        Objects.requireNonNull(
                value,
                "Card amount cannot be null."
        );

        if (value.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Card daily limit must be greater than zero."
            );
        }

        BigInteger minorUnits;

        try {
            minorUnits =
                    value.setScale(2, RoundingMode.UNNECESSARY)
                            .movePointRight(2)
                            .toBigIntegerExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Card amount must have at most 2 decimal places.",
                    exception
            );
        }

        String digits = minorUnits.toString();

        if (digits.length() > 15) {
            throw new IllegalArgumentException(
                    "Card amount exceeds the mainframe field length."
            );
        }

        return "0".repeat(15 - digits.length())
                + digits;
    }

    private void validateAccountId(
            String accountId
    ) {

        if (accountId == null
                || !accountId.matches("A\\d{12}")) {
            throw new IllegalArgumentException(
                    "Account ID must match A followed by 12 digits."
            );
        }
    }

    private void validateCardId(
            String cardId
    ) {

        if (cardId == null
                || !cardId.matches("K\\d{12}")) {
            throw new IllegalArgumentException(
                    "Card ID must match K followed by 12 digits."
            );
        }
    }

    private void validateStatus(
            String status
    ) {

        if (!"A".equals(status)
                && !"I".equals(status)) {
            throw new IllegalArgumentException(
                    "Status must be A or I."
            );
        }
    }

    private void requireLength(
            String record,
            int expectedLength,
            String description
    ) {

        if (record.length() != expectedLength) {
            throw new IllegalStateException(
                    description
                            + " must have length "
                            + expectedLength
                            + ", got "
                            + record.length()
            );
        }
    }
}
