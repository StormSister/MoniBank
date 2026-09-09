package com.monibank.mainframe.dashboard;

import com.monibank.mainframe.dashboard.api.DailyCloseReportResponse;
import com.monibank.mainframe.dashboard.mainframe.DailyStatisticsRecordMapper;
import com.monibank.mainframe.dashboard.mainframe.DailyStatisticsResultParser;
import com.monibank.mainframe.hercules.KicksMainframeOperationExecutor;
import com.monibank.mainframe.model.MainframeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyStatisticsService {

    private static final String CALCULATE_OPERATION = "DAYSTAT";
    private static final String LOAD_OPERATION = "GETSTAT";

    private final DailyStatisticsRecordMapper recordMapper;
    private final DailyStatisticsResultParser resultParser;
    private final KicksMainframeOperationExecutor operationExecutor;

    public DailyCloseReportResponse calculate(
            LocalDate businessDate,
            String currency
    ) {
        return execute(
                CALCULATE_OPERATION,
                businessDate,
                currency
        );
    }

    public DailyCloseReportResponse load(
            LocalDate businessDate,
            String currency
    ) {
        try {
            return execute(
                    LOAD_OPERATION,
                    businessDate,
                    currency
            );
        } catch (IllegalStateException exception) {
            if (isReportNotFound(exception)) {
                throw new DailyCloseReportNotFoundException(
                        businessDate,
                        currency,
                        exception
                );
            }

            throw new DailyCloseReportUnavailableException(
                    "Could not load closed-day report "
                            + businessDate
                            + " "
                            + currency
                            + " from MVS.",
                    exception
            );
        }
    }

    private DailyCloseReportResponse execute(
            String operation,
            LocalDate businessDate,
            String currency
    ) {
        String input = recordMapper.toRecord(businessDate, currency);

        MainframeResult result = operationExecutor.execute(
                operation,
                input
        );

        DailyCloseReportResponse report = resultParser.parse(result);

        if (!businessDate.equals(report.businessDate())) {
            throw new DailyCloseFailedException(
                    operation + " returned date " + report.businessDate()
                            + " instead of " + businessDate + "."
            );
        }

        if (!currency.equals(report.currency())) {
            throw new DailyCloseFailedException(
                    operation + " returned currency " + report.currency()
                            + " instead of " + currency + "."
            );
        }

        log.info(
                "{} [{}] returned {} operation(s) for {} {}",
                operation,
                report.requestId(),
                report.transactions().operationCount(),
                businessDate,
                currency
        );

        return report;
    }

    private boolean isReportNotFound(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().contains("RPTNOTF")) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }
}
