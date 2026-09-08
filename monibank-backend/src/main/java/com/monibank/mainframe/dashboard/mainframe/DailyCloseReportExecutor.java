package com.monibank.mainframe.dashboard.mainframe;

import com.monibank.mainframe.dashboard.DailyCloseReportUnavailableException;
import com.monibank.mainframe.dashboard.api.DailyCloseReportResponse;
import com.monibank.mainframe.hercules.MainframeRequestIdGenerator;
import com.monibank.mainframe.hercules.MainframeTcpResultListener;
import com.monibank.mainframe.hercules.jcl.DailyCloseReportJclFactory;
import com.monibank.mainframe.port.MainframeGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
@RequiredArgsConstructor
public class DailyCloseReportExecutor {

    private static final Duration REPORT_TIMEOUT =
            Duration.ofSeconds(30);

    private final MainframeRequestIdGenerator requestIdGenerator;
    private final DailyCloseReportJclFactory jclFactory;
    private final MainframeGateway mainframeGateway;
    private final MainframeTcpResultListener tcpResultListener;
    private final DailyCloseReportParser reportParser;

    public DailyCloseReportResponse execute(
            LocalDate businessDate,
            String expectedCurrency
    ) {

        String requestId =
                requestIdGenerator.next();

        String jcl =
                jclFactory.create(
                        requestId,
                        businessDate
                );

        tcpResultListener.registerDailyReport(requestId);

        try {

            log.info(
                    "DAILY REPORT [{}] loading {} {}",
                    requestId,
                    businessDate,
                    expectedCurrency
            );

            mainframeGateway.submitJcl(jcl);

            List<String> records =
                    tcpResultListener.awaitDailyReport(
                            requestId,
                            REPORT_TIMEOUT
                    );

            DailyCloseReportResponse report =
                    reportParser.parse(
                            requestId,
                            records
                    );

            validateRequestedReport(
                    report,
                    businessDate,
                    expectedCurrency
            );

            log.info(
                    "DAILY REPORT [{}] loaded successfully",
                    requestId
            );

            return report;

        } catch (TimeoutException exception) {

            throw new DailyCloseReportUnavailableException(
                    "Timed out waiting for daily close report "
                            + businessDate
                            + ".",
                    exception
            );

        } catch (DailyCloseReportUnavailableException exception) {

            throw exception;

        } catch (Exception exception) {

            throw new DailyCloseReportUnavailableException(
                    "Could not load daily close report "
                            + businessDate
                            + ".",
                    exception
            );

        } finally {

            tcpResultListener.unregisterDailyReport(requestId);
        }
    }

    private void validateRequestedReport(
            DailyCloseReportResponse report,
            LocalDate expectedDate,
            String expectedCurrency
    ) {

        if (!expectedDate.equals(report.businessDate())) {
            throw new IllegalStateException(
                    "Daily report date "
                            + report.businessDate()
                            + " does not match requested date "
                            + expectedDate
                            + "."
            );
        }

        if (!expectedCurrency.equals(report.currency())) {
            throw new IllegalStateException(
                    "Daily report currency "
                            + report.currency()
                            + " does not match requested currency "
                            + expectedCurrency
                            + "."
            );
        }
    }
}
