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

    private static final String OPERATION = "DAYSTAT";

    private final DailyStatisticsRecordMapper recordMapper;
    private final DailyStatisticsResultParser resultParser;
    private final KicksMainframeOperationExecutor operationExecutor;

    public DailyCloseReportResponse calculate(
            LocalDate businessDate,
            String currency
    ) {
        String input = recordMapper.toRecord(businessDate, currency);

        MainframeResult result = operationExecutor.execute(
                OPERATION,
                input
        );

        DailyCloseReportResponse report = resultParser.parse(result);

        if (!businessDate.equals(report.businessDate())) {
            throw new DailyCloseFailedException(
                    "DAYSTAT returned date " + report.businessDate()
                            + " instead of " + businessDate + "."
            );
        }

        if (!currency.equals(report.currency())) {
            throw new DailyCloseFailedException(
                    "DAYSTAT returned currency " + report.currency()
                            + " instead of " + currency + "."
            );
        }

        log.info(
                "DAYSTAT [{}] calculated {} operation(s) for {} {}",
                report.requestId(),
                report.transactions().operationCount(),
                businessDate,
                currency
        );

        return report;
    }
}
