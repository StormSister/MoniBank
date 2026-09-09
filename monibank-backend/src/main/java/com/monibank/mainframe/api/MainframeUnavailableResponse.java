package com.monibank.mainframe.api;

import java.time.Instant;

public record MainframeUnavailableResponse(
        Instant timestamp,
        int status,
        String code,
        String message
) {
}
