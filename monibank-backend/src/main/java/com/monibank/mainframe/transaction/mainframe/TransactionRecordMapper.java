package com.monibank.mainframe.transaction.mainframe;

import com.monibank.mainframe.transaction.api.CashTransactionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
public class TransactionRecordMapper {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String toDepositRecord(
            CashTransactionRequest request
    ) {
        return toRecord(request, "DP");
    }

    public String toWithdrawalRecord(
            CashTransactionRequest request
    ) {
        return toRecord(request, "WD");
    }

    private String toRecord(
            CashTransactionRequest request,
            String type
    ) {

        Objects.requireNonNull(
                request,
                "Cash transaction request cannot be null."
        );

        validateAccountId(request.accountId());

        String createdAt =
                LocalDateTime.now()
                        .format(TIMESTAMP_FORMAT);

        String record =
                request.accountId()
                        + type
                        + amount(request.amount())
                        + fixed(request.detail(), 34)
                        + fixed(request.sourceId(), 13)
                        + createdAt;

        requireLength(record, 91);

        return record;
    }

    private String amount(
            BigDecimal value
    ) {

        Objects.requireNonNull(
                value,
                "Transaction amount cannot be null."
        );

        if (value.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Transaction amount must be positive."
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
                    "Transaction amount must have at most 2 decimal places.",
                    exception
            );
        }

        String digits = minorUnits.toString();

        if (digits.length() > 15) {
            throw new IllegalArgumentException(
                    "Transaction amount exceeds the mainframe field length."
            );
        }

        return "0".repeat(15 - digits.length())
                + digits;
    }

    private String fixed(
            String value,
            int length
    ) {

        String safeValue =
                value == null
                        ? ""
                        : value;

        if (safeValue.length() > length) {
            throw new IllegalArgumentException(
                    "Value exceeds mainframe field length "
                            + length
                            + ": "
                            + safeValue
            );
        }

        if (safeValue.chars().anyMatch(
                Character::isISOControl
        )) {
            throw new IllegalArgumentException(
                    "Mainframe field contains a control character."
            );
        }

        return String.format(
                "%-" + length + "s",
                safeValue
        );
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

    private void requireLength(
            String record,
            int expectedLength
    ) {

        if (record.length() != expectedLength) {
            throw new IllegalStateException(
                    "Transaction input must have length "
                            + expectedLength
                            + ", got "
                            + record.length()
            );
        }
    }
}
