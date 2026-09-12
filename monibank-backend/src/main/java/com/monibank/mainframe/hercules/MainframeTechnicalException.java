package com.monibank.mainframe.hercules;

import org.springframework.http.HttpStatus;

public final class MainframeTechnicalException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final String requestId;
    private final String operation;
    private final HttpStatus status;
    private final boolean retryable;

    private MainframeTechnicalException(
            String code,
            String requestId,
            String operation,
            String message,
            HttpStatus status,
            boolean retryable,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.requestId = requestId;
        this.operation = operation;
        this.status = status;
        this.retryable = retryable;
    }

    public static MainframeTechnicalException unavailable(
            String code,
            String requestId,
            String operation,
            String message,
            Throwable cause
    ) {
        return new MainframeTechnicalException(
                code,
                requestId,
                operation,
                message,
                HttpStatus.SERVICE_UNAVAILABLE,
                true,
                cause
        );
    }

    public static MainframeTechnicalException gatewayTimeout(
            String code,
            String requestId,
            String operation,
            String message,
            Throwable cause
    ) {
        return new MainframeTechnicalException(
                code,
                requestId,
                operation,
                message,
                HttpStatus.GATEWAY_TIMEOUT,
                true,
                cause
        );
    }

    public static MainframeTechnicalException badGateway(
            String code,
            String requestId,
            String operation,
            String message,
            Throwable cause
    ) {
        return new MainframeTechnicalException(
                code,
                requestId,
                operation,
                message,
                HttpStatus.BAD_GATEWAY,
                false,
                cause
        );
    }

    public String code() {
        return code;
    }

    public String requestId() {
        return requestId;
    }

    public String operation() {
        return operation;
    }

    public HttpStatus status() {
        return status;
    }

    public boolean retryable() {
        return retryable;
    }
}
