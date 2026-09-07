package com.monibank.mainframe.customer.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCustomerRequest(

        @NotBlank
        @Pattern(regexp = "[A-Z]{2}")
        String countryCode,

        @NotBlank
        @Pattern(regexp = "[0-9]{11}")
        String nationalId,

        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "[A-Za-z][A-Za-z .'-]*")
        String firstName,

        @NotBlank
        @Size(max = 40)
        @Pattern(regexp = "[A-Za-z][A-Za-z .'-]*")
        String lastName,

        @NotNull
        @PastOrPresent
        @JsonFormat(pattern = "yyyyMMdd")
        LocalDate dateOfBirth
) {
}