package com.monibank.mainframe.customer.mainframe;

import com.monibank.mainframe.model.MainframeDatasetMode;
import com.monibank.mainframe.model.MainframeDatasetSpec;
import com.monibank.mainframe.model.MainframeOperationSpec;
import com.monibank.mainframe.model.MainframeOperationType;

import java.util.List;

public final class CustomerMainframeOperations {

    private CustomerMainframeOperations() {
    }

    public static final MainframeOperationSpec ADD_CUSTOMER =
            new MainframeOperationSpec(
                    "ADD_CUSTOMER",
                    "ADDCUST",
                    "MBANK.CUST",
                    126,
                    119,
                    MainframeOperationType.WRITE,
                    List.of(
                            new MainframeDatasetSpec(
                                    "CUSTFILE",
                                    "MBANK.CUST",
                                    MainframeDatasetMode.UPDATE
                            )
                    )
            );
//    public static final MainframeOperationSpec LIST_CUSTOMERS =
//            new MainframeOperationSpec(
//                    "LIST_CUSTOMERS",
//                    null,
//                    "MBANK.CUST",
//                    0,
//                    119,
//                    MainframeOperationType.READ_ALL
//            );

    public static final MainframeOperationSpec CHANGE_STATUS =
            new MainframeOperationSpec(
                    "CHANGE_CUSTOMER_STATUS",
                    "CHGCUST",
                    "MBANK.CUST",
                    22,  // requestId 8 + customerId 13 + status 1
                    119,
                    MainframeOperationType.UPDATE,
                    List.of(
                            new MainframeDatasetSpec(
                                    "CUSTFILE",
                                    "MBANK.CUST",
                                    MainframeDatasetMode.UPDATE
                            )
                    )
            );

    public static final MainframeOperationSpec LIST_CUSTOMERS =
            new MainframeOperationSpec(
                    "LIST_CUSTOMERS",
                    "LISTCUST",
                    "MBANK.CUST",
                    8,
                    119,
                    MainframeOperationType.READ_ALL,
                    List.of(
                            new MainframeDatasetSpec(
                                    "CUSTFILE",
                                    "MBANK.CUST",
                                    MainframeDatasetMode.READ
                            )
                    )
            );
}