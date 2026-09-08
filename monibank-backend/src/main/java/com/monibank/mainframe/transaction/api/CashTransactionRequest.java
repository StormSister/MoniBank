package com.monibank.mainframe.transaction.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CashTransactionRequest(

        @NotBlank
        @Pattern(regexp = "A\\d{12}")
        String accountId,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @NotBlank
        @Size(max = 34)
        @Pattern(regexp = "[A-Za-z0-9 .#/_'-]+")
        String detail,

        @NotBlank
        @Size(max = 13)
        @Pattern(regexp = "[A-Za-z0-9#_-]+")
        String sourceId
) {
}
