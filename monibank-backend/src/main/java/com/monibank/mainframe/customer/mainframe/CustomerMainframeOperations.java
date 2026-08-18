package com.monibank.mainframe.customer.mainframe;

import com.monibank.mainframe.model.MainframeOperationSpec;
import com.monibank.mainframe.model.MainframeOperationType;

public final class CustomerMainframeOperations {

    private CustomerMainframeOperations() {
    }

    public static final MainframeOperationSpec ADD_CUSTOMER =
            new MainframeOperationSpec(
                    "ADD_CUSTOMER",
                    "ADDCUST",
                    "MBANK.CUST",
                    119,
                    MainframeOperationType.WRITE
            );

    public static final MainframeOperationSpec LIST_CUSTOMERS =
            new MainframeOperationSpec(
                    "LIST_CUSTOMERS",
                    null,
                    "MBANK.CUST",
                    119,
                    MainframeOperationType.READ_ALL
            );

    public static final MainframeOperationSpec CHANGE_STATUS =
            new MainframeOperationSpec(
                    "CHANGE_CUSTOMER_STATUS",
                    "CHGCUST",
                    "MBANK.CUST",
                    14,
                    MainframeOperationType.UPDATE
            );
}