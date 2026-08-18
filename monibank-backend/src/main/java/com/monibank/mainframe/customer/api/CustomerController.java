package com.monibank.mainframe.customer.api;


import com.monibank.mainframe.customer.CustomerService;
import com.monibank.mainframe.model.MainframeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<String> createCustomer(
            @RequestBody CreateCustomerRequest request
    ) {

        String jobName =
                customerService.createCustomer(request);

        return ResponseEntity.accepted()
                .body(jobName);
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getCustomers() {

        return ResponseEntity.ok(
                customerService.getCustomers()
        );
    }

    @PatchMapping("/{customerId}/status")
    public ResponseEntity<MainframeResult> changeStatus(
            @PathVariable String customerId,
            @RequestBody ChangeCustomerStatusRequest request
    ) {

        return ResponseEntity.ok(
                customerService.changeStatus(
                        customerId,
                        request.status()
                )
        );
    }
}