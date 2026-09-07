package com.monibank.mainframe.card.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeCardStatusRequest(
        @NotBlank
        @Pattern(regexp = "[AI]")
        String status
) {
}
