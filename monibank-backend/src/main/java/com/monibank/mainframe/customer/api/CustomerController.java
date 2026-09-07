package com.monibank.mainframe.customer.api;

import com.monibank.mainframe.customer.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request
    ) {

        return ResponseEntity.ok(
                customerService.createCustomer(request)
        );
    }

    @PostMapping("/get")
    public ResponseEntity<CustomerResponse> getCustomer(
            @Valid @RequestBody GetCustomerRequest request
    ) {

        return ResponseEntity.ok(
                customerService.getCustomer(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getCustomers() {

        return ResponseEntity.ok(
                customerService.getCustomers()
        );
    }

    @PatchMapping("/{customerId}/status")
    public ResponseEntity<CustomerResponse> changeStatus(
            @PathVariable
            @Pattern(regexp = "C\\d{12}")
            String customerId,
            @Valid @RequestBody ChangeCustomerStatusRequest request
    ) {

        return ResponseEntity.ok(
                customerService.changeStatus(
                        customerId,
                        request.status()
                )
        );
    }
}
