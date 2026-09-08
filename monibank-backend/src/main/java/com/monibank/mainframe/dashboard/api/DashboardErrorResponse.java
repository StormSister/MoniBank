package com.monibank.mainframe.dashboard.api;

import java.time.Instant;

public record DashboardErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message
) {
}
