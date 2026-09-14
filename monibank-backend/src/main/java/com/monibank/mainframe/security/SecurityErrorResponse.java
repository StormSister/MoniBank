package com.monibank.mainframe.security;

import java.time.Instant;

public record SecurityErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        boolean retryable
) {
}
