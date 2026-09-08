package com.monibank.mainframe.dashboard;

public class DailyCloseReportUnavailableException
        extends RuntimeException {

    public DailyCloseReportUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
