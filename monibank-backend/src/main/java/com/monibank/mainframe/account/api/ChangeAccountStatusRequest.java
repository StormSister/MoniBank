package com.monibank.mainframe.account.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeAccountStatusRequest(
        @NotBlank
        @Pattern(regexp = "[AI]")
        String status
) {
}
