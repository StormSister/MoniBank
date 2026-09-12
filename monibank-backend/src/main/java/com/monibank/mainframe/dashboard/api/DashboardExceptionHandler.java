package com.monibank.mainframe.dashboard.api;

import com.monibank.mainframe.dashboard.DailyCloseReportUnavailableException;
import com.monibank.mainframe.dashboard.DailyCloseReportNotFoundException;
import com.monibank.mainframe.api.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = DailyCloseReportController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DashboardExceptionHandler {

    @ExceptionHandler(DailyCloseReportNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            DailyCloseReportNotFoundException exception
    ) {

        HttpStatus status = HttpStatus.NOT_FOUND;

        return ResponseEntity
                .status(status)
                .body(
                        new ApiErrorResponse(
                                Instant.now(),
                                status.value(),
                                "DAILY_CLOSE_REPORT_NOT_FOUND",
                                exception.getMessage(),
                                null,
                                null,
                                false,
                                null
                        )
                );
    }

    @ExceptionHandler(DailyCloseReportUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnavailable(
            DailyCloseReportUnavailableException exception
    ) {

        HttpStatus status =
                HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity
                .status(status)
                .body(
                        new ApiErrorResponse(
                                Instant.now(),
                                status.value(),
                                "DAILY_CLOSE_REPORT_UNAVAILABLE",
                                exception.getMessage(),
                                null,
                                null,
                                true,
                                null
                        )
                );
    }
}
