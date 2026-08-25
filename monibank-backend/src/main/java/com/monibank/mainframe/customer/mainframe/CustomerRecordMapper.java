package com.monibank.mainframe.customer.mainframe;

import com.monibank.mainframe.customer.api.CreateCustomerRequest;
import com.monibank.mainframe.mapping.MainframeRecordMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CustomerRecordMapper
        implements MainframeRecordMapper<CreateCustomerRequest> {

    private static final DateTimeFormatter CREATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String toRecord(
            CreateCustomerRequest request
    ) {

        String record =
                fixed(request.countryCode(), 2)
                        + fixed(request.nationalId(), 11)
                        + fixed(request.firstName(), 30)
                        + fixed(request.lastName(), 40)
                        + fixed(request.dateOfBirth(), 8)
                        + fixed(
                        LocalDateTime.now()
                                .format(CREATED_AT_FORMAT),
                        14
                );

        if (record.length() != 105) {
            throw new IllegalStateException(
                    "Customer data must have length 105, got "
                            + record.length()
            );
        }

        return record;
    }

    public String toCreateRecord(
            String requestId,
            String customerId,
            CreateCustomerRequest request
    ) {

        validateRequestId(requestId);
        validateCustomerId(customerId);

        String record =
                requestId
                        + customerId
                        + toRecord(request);

        if (record.length() != 126) {
            throw new IllegalStateException(
                    "Create customer mainframe request must have length 126, got "
                            + record.length()
            );
        }

        return record;
    }

    public String toStatusUpdateRecord(
            String requestId,
            String customerId,
            String status
    ) {

        validateRequestId(requestId);
        validateCustomerId(customerId);

        if (!"A".equals(status)
                && !"I".equals(status)) {
            throw new IllegalArgumentException(
                    "Status must be A or I"
            );
        }

        String record =
                requestId
                        + customerId
                        + status;

        if (record.length() != 22) {
            throw new IllegalStateException(
                    "Customer status update record must have length 22, got "
                            + record.length()
            );
        }

        return record;
    }

    private void validateRequestId(
            String requestId
    ) {

        if (requestId == null
                || requestId.length() != 8) {
            throw new IllegalArgumentException(
                    "Request ID must have exactly 8 characters"
            );
        }
    }

    private void validateCustomerId(
            String customerId
    ) {

        if (customerId == null
                || customerId.length() != 13) {
            throw new IllegalArgumentException(
                    "Customer ID must have exactly 13 characters"
            );
        }

        if (!customerId.matches("C\\d{12}")) {
            throw new IllegalArgumentException(
                    "Customer ID must match C followed by 12 digits"
            );
        }
    }

    private String fixed(
            String value,
            int length
    ) {

        String safeValue =
                value == null ? "" : value;

        if (safeValue.length() > length) {
            throw new IllegalArgumentException(
                    "Value too long for mainframe field: "
                            + safeValue
            );
        }

        return String.format(
                "%-" + length + "s",
                safeValue
        );
    }
}