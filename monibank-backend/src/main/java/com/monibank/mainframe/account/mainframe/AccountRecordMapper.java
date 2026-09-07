package com.monibank.mainframe.account.mainframe;

import com.monibank.mainframe.account.api.CreateAccountRequest;
import com.monibank.mainframe.mapping.MainframeRecordMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
public class AccountRecordMapper
        implements MainframeRecordMapper<CreateAccountRequest> {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String toRecord(
            CreateAccountRequest request
    ) {

        Objects.requireNonNull(
                request,
                "Create account request cannot be null."
        );

        validateCustomerId(request.customerId());
        validateAccountType(request.type());

        String createdAt =
                LocalDateTime.now()
                        .format(TIMESTAMP_FORMAT);

        String record =
                request.customerId()
                        + request.type()
                        + amount(request.overdraftLimit())
                        + createdAt;

        requireLength(
                record,
                44,
                "Account data"
        );

        return record;
    }

    public String toStatusUpdateRecord(
            String accountId,
            String status
    ) {

        validateAccountId(accountId);
        validateStatus(status);

        String updatedAt =
                LocalDateTime.now()
                        .format(TIMESTAMP_FORMAT);

        String record =
                accountId
                        + status
                        + updatedAt;

        requireLength(
                record,
                28,
                "Account status update record"
        );

        return record;
    }

    private String amount(
            BigDecimal value
    ) {

        Objects.requireNonNull(
                value,
                "Account amount cannot be null."
        );

        if (value.signum() < 0) {
            throw new IllegalArgumentException(
                    "Account amount cannot be negative."
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
                    "Account amount must have at most 2 decimal places.",
                    exception
            );
        }

        String digits = minorUnits.toString();

        if (digits.length() > 15) {
            throw new IllegalArgumentException(
                    "Account amount exceeds the mainframe field length."
            );
        }

        return "0".repeat(15 - digits.length())
                + digits;
    }

    private void validateCustomerId(
            String customerId
    ) {

        if (customerId == null
                || !customerId.matches("C\\d{12}")) {
            throw new IllegalArgumentException(
                    "Customer ID must match C followed by 12 digits."
            );
        }
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

    private void validateAccountType(
            String type
    ) {

        if (!"ST".equals(type)
                && !"OD".equals(type)) {
            throw new IllegalArgumentException(
                    "Account type must be ST or OD."
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
