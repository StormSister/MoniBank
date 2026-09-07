package com.monibank.mainframe.account.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotBlank
        @Pattern(regexp = "C\\d{12}")
        String customerId,

        @NotBlank
        @Pattern(regexp = "ST|OD")
        String type,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 13, fraction = 2)
        BigDecimal overdraftLimit
) {
}
