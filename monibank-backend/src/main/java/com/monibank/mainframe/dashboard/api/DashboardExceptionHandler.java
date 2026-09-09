package com.monibank.mainframe.dashboard.api;

import com.monibank.mainframe.dashboard.DailyCloseReportUnavailableException;
import com.monibank.mainframe.dashboard.DailyCloseReportNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = DailyCloseReportController.class)
public class DashboardExceptionHandler {

    @ExceptionHandler(DailyCloseReportNotFoundException.class)
    public ResponseEntity<DashboardErrorResponse> handleNotFound(
            DailyCloseReportNotFoundException exception
    ) {

        HttpStatus status = HttpStatus.NOT_FOUND;

        return ResponseEntity
                .status(status)
                .body(
                        new DashboardErrorResponse(
                                Instant.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                exception.getMessage()
                        )
                );
    }

    @ExceptionHandler(DailyCloseReportUnavailableException.class)
    public ResponseEntity<DashboardErrorResponse> handleUnavailable(
            DailyCloseReportUnavailableException exception
    ) {

        HttpStatus status =
                HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity
                .status(status)
                .body(
                        new DashboardErrorResponse(
                                Instant.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                exception.getMessage()
                        )
                );
    }
}
