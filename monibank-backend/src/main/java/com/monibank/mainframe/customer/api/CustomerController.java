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
    public ResponseEntity<MainframeResult> createCustomer(
            @RequestBody CreateCustomerRequest request
    ) {

        return ResponseEntity.ok(
                customerService.createCustomer(request)
        );
    }

    @GetMapping
    public ResponseEntity<MainframeResult> getCustomers() {

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