package com.monibank.mainframe.api;

import com.monibank.mainframe.dashboard.DailyCloseFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MainframeUnavailableExceptionHandler {

    @ExceptionHandler(DailyCloseFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleCloseFailure(
            DailyCloseFailedException exception
    ) {
        return unavailable(
                "DAILY_CLOSE_FAILED",
                exception.getMessage()
        );
    }

    private ResponseEntity<ApiErrorResponse> unavailable(
            String code,
            String message
    ) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(status).body(
                new ApiErrorResponse(
                        Instant.now(),
                        status.value(),
                        code,
                        message,
                        null,
                        null,
                        true,
                        null
                )
        );
    }
}
