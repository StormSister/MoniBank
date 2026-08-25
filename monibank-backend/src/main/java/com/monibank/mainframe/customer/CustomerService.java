package com.monibank.mainframe.customer;

import com.monibank.mainframe.customer.api.CreateCustomerRequest;
import com.monibank.mainframe.customer.api.CustomerResponse;
import com.monibank.mainframe.customer.mainframe.CustomerMainframeOperations;
import com.monibank.mainframe.customer.mainframe.CustomerRecordMapper;
import com.monibank.mainframe.customer.mainframe.CustomerRecordParser;
import com.monibank.mainframe.hercules.MainframeIdGenerator;
import com.monibank.mainframe.hercules.MainframeOperationExecutor;
import com.monibank.mainframe.hercules.MainframeRequestIdGenerator;
import com.monibank.mainframe.model.MainframeDataRecord;
import com.monibank.mainframe.model.MainframeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRecordMapper customerRecordMapper;
    private final CustomerRecordParser customerRecordParser;
    private final MainframeRequestIdGenerator requestIdGenerator;
    private final MainframeOperationExecutor mainframeOperationExecutor;
    private final MainframeIdGenerator mainframeIdGenerator;

    public MainframeResult createCustomer(
            CreateCustomerRequest request
    ) {

        String requestId =
                requestIdGenerator.next();

        String customerId =
                "C" + mainframeIdGenerator.nextNumber();

        String inputRecord =
                customerRecordMapper.toCreateRecord(
                        requestId,
                        customerId,
                        request
                );

        return mainframeOperationExecutor.execute(
                requestId,
                CustomerMainframeOperations.ADD_CUSTOMER,
                inputRecord
        );
    }

    public MainframeResult changeStatus(
            String customerId,
            String status
    ) {

        String requestId =
                requestIdGenerator.next();

        String inputRecord =
                customerRecordMapper.toStatusUpdateRecord(
                        requestId,
                        customerId,
                        status
                );

        return mainframeOperationExecutor.execute(
                requestId,
                CustomerMainframeOperations.CHANGE_STATUS,
                inputRecord
        );
    }

    public MainframeResult getCustomers() {

        String requestId =
                requestIdGenerator.next();

        return mainframeOperationExecutor.execute(
                requestId,
                CustomerMainframeOperations.LIST_CUSTOMERS,
                requestId
        );
    }

    private boolean isCustomer(
            MainframeDataRecord record
    ) {

        return "CUSTOMER".equals(
                record.entityType()
        );
    }

    private String generateCustomerId() {

        MainframeResult result =
                getCustomers();

        long maxId =
                result.data()
                        .stream()
                        .filter(this::isCustomer)
                        .map(MainframeDataRecord::payload)
                        .map(customerRecordParser::parse)
                        .map(CustomerResponse::customerId)
                        .filter(id ->
                                id != null
                                        && id.startsWith("C")
                        )
                        .map(id ->
                                id.substring(1)
                        )
                        .mapToLong(Long::parseLong)
                        .max()
                        .orElse(0L);

        return "C"
                + String.format(
                "%012d",
                maxId + 1
        );
    }
}