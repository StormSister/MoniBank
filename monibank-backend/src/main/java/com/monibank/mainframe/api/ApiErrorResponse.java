package com.monibank.mainframe.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String requestId,
        String operation,
        boolean retryable,
        Map<String, String> fieldErrors
) {
}
