package com.monibank.mainframe.dashboard;

public class DailyCloseFailedException extends IllegalStateException {

    public DailyCloseFailedException(String message) {
        super(message);
    }

    public DailyCloseFailedException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
