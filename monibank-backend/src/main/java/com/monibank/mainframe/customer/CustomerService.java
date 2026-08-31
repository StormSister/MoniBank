package com.monibank.mainframe.customer;

import com.monibank.mainframe.customer.api.CreateCustomerRequest;
import com.monibank.mainframe.customer.api.CustomerResponse;
import com.monibank.mainframe.customer.api.GetCustomerRequest;
import com.monibank.mainframe.customer.mainframe.CustomerMainframeOperations;
import com.monibank.mainframe.customer.mainframe.CustomerRecordMapper;
import com.monibank.mainframe.customer.mainframe.CustomerRecordParser;
import com.monibank.mainframe.hercules.KicksMainframeOperationExecutor;
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
    private final KicksMainframeOperationExecutor
            kicksMainframeOperationExecutor;
    private final MainframeIdGenerator mainframeIdGenerator;

    public CustomerResponse createCustomer(
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

        MainframeResult result =
                mainframeOperationExecutor.execute(
                        requestId,
                        CustomerMainframeOperations.ADD_CUSTOMER,
                        inputRecord
                );

        return parseSingleCustomer(
                result,
                customerId,
                "ADDCUST"
        );
    }

    public CustomerResponse getCustomer(
            GetCustomerRequest request
    ) {

        MainframeResult result =
                kicksMainframeOperationExecutor.execute(
                        CustomerMainframeOperations.GET_CUSTOMER,
                        request.customerId()
                );

        return parseSingleCustomer(
                result,
                request.customerId(),
                "GETCUST"
        );
    }

    public CustomerResponse changeStatus(
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

        MainframeResult result =
                mainframeOperationExecutor.execute(
                        requestId,
                        CustomerMainframeOperations.CHANGE_STATUS,
                        inputRecord
                );

        return parseSingleCustomer(
                result,
                customerId,
                "CHGCUST"
        );
    }

    public List<CustomerResponse> getCustomers() {

        String requestId =
                requestIdGenerator.next();

        MainframeResult result =
                mainframeOperationExecutor.execute(
                        requestId,
                        CustomerMainframeOperations.LIST_CUSTOMERS,
                        requestId
                );

        return parseCustomers(result);
    }

    private boolean isCustomer(
            MainframeDataRecord record
    ) {

        return "CUSTOMER".equals(
                record.entityType()
        );
    }

    private List<CustomerResponse> parseCustomers(
            MainframeResult result
    ) {

        return result.data()
                .stream()
                .filter(this::isCustomer)
                .map(MainframeDataRecord::payload)
                .map(customerRecordParser::parse)
                .toList();
    }

    private CustomerResponse parseSingleCustomer(
            MainframeResult result,
            String expectedCustomerId,
            String operation
    ) {

        List<CustomerResponse> customers =
                parseCustomers(result);

        if (customers.size() != 1) {
            throw new IllegalStateException(
                    operation
                            + " returned "
                            + customers.size()
                            + " CUSTOMER records; expected 1"
            );
        }

        CustomerResponse customer =
                customers.getFirst();

        if (!expectedCustomerId.equals(
                customer.customerId()
        )) {
            throw new IllegalStateException(
                    operation
                            + " returned customer "
                            + customer.customerId()
                            + ", expected "
                            + expectedCustomerId
            );
        }

        return customer;
    }
}
