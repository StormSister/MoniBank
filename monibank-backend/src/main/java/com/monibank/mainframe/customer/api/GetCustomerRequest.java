package com.monibank.mainframe.customer.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GetCustomerRequest(
        @NotBlank(
                message = "Customer ID cannot be blank."
        )
        @Pattern(
                regexp = "^C[0-9]{12}$",
                message = "Customer ID must contain C and 12 digits."
        )
        String customerId
) {
}
