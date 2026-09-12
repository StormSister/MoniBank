package com.monibank.mainframe.hercules;

import java.util.Locale;

public final class MainframeBusinessException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final String requestId;
    private final String operation;

    public MainframeBusinessException(
            String code,
            String requestId,
            String operation
    ) {
        super(
                "Mainframe rejected " + operation
                        + " with code " + normalize(code)
        );
        this.code = normalize(code);
        this.requestId = requestId;
        this.operation = operation;
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

    private static String normalize(String code) {
        if (code == null || code.isBlank()) {
            return "MAINFRAME_ERROR";
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
