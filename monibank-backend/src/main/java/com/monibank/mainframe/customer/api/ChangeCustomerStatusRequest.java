package com.monibank.mainframe.customer.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeCustomerStatusRequest(
        @NotBlank
        @Pattern(regexp = "[AI]")
        String status
) {
}
