package com.monibank.mainframe.customer.mainframe;

import com.monibank.mainframe.customer.api.CustomerResponse;
import org.springframework.stereotype.Component;

@Component
public class CustomerRecordParser {

    public CustomerResponse parse(String record) {

        if (record == null || record.length() < 119) {
            throw new IllegalArgumentException(
                    "Invalid customer record length: "
                            + (record == null ? 0 : record.length())
            );
        }

        return new CustomerResponse(
                field(record, 1, 14),     // customerId
                field(record, 14, 16),    // countryCode
                field(record, 16, 27),    // nationalId
                field(record, 27, 57),    // firstName
                field(record, 57, 97),    // lastName
                field(record, 97, 105),   // dateOfBirth
                field(record, 0, 1),      // status
                field(record, 105, 119)   // createdAt
        );
    }

    private String field(
            String record,
            int from,
            int to
    ) {
        return record.substring(from, to).trim();
    }
}