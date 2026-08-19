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
    public String toRecord(CreateCustomerRequest request) {

        String record =
                fixed("A", 1)
                        + fixed(request.customerId(), 13)
                        + fixed(request.countryCode(), 2)
                        + fixed(request.nationalId(), 11)
                        + fixed(request.firstName(), 30)
                        + fixed(request.lastName(), 40)
                        + fixed(request.dateOfBirth(), 8)
                        + fixed(
                        LocalDateTime.now()
                                .format(CREATED_AT_FORMAT),
                        14
                );

        if (record.length() != 119) {
            throw new IllegalStateException(
                    "Customer mainframe record must have length 119, got "
                            + record.length()
            );
        }

        return record;
    }

    private String fixed(String value, int length) {

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

    public String toStatusUpdateRecord(
            String requestId,
            String customerId,
            String status
    ) {

        if (requestId == null
                || requestId.length() != 8) {
            throw new IllegalArgumentException(
                    "Request ID must have exactly 8 characters"
            );
        }

        if (customerId == null
                || customerId.length() != 13) {
            throw new IllegalArgumentException(
                    "Customer ID must have exactly 13 characters"
            );
        }

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
}