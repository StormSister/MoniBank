package com.monibank.mainframe.card.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.YearMonth;

public record CreateCardRequest(

        @NotBlank
        @Pattern(regexp = "A\\d{12}")
        String accountId,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal dailyLimit,

        @NotNull
        @FutureOrPresent
        @JsonFormat(pattern = "yyyyMM")
        YearMonth expiry
) {
}
