package com.monibank.mainframe.dashboard.mainframe;

import com.monibank.mainframe.config.DailyCloseProperties;
import com.monibank.mainframe.dashboard.DailyCloseFailedException;
import com.monibank.mainframe.dashboard.api.DailyCloseReportResponse;
import com.monibank.mainframe.hercules.MainframeRequestIdGenerator;
import com.monibank.mainframe.hercules.MainframeTcpResultListener;
import com.monibank.mainframe.hercules.jcl.DailyCloseJclFactory;
import com.monibank.mainframe.port.MainframeGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
@RequiredArgsConstructor
public class DailyCloseBatchExecutor {

    private final DailyCloseProperties properties;
    private final MainframeRequestIdGenerator requestIdGenerator;
    private final DailyCloseJclFactory jclFactory;
    private final MainframeGateway mainframeGateway;
    private final MainframeTcpResultListener tcpResultListener;
    private final DailyCloseReportParser reportParser;

    public DailyCloseReportResponse execute(
            LocalDate businessDate,
            String currency
    ) {
        String requestId = requestIdGenerator.next();
        String jcl = jclFactory.create(requestId, businessDate, currency);

        /*
         * CHECK runs only when EXTRACT and DAILY both finish with RC=0000.
         * Therefore receiving a complete, valid MBS report from CHECK proves
         * that all three close steps completed successfully.
         */
        tcpResultListener.registerDailyReport(requestId);

        try {
            log.info(
                    "DAILY CLOSE [{}] submitting {} {}",
                    requestId,
                    businessDate,
                    currency
            );

            mainframeGateway.submitJcl(jcl);

            List<String> records =
                    tcpResultListener.awaitDailyReport(
                            requestId,
                            properties.batchTimeout()
                    );

            DailyCloseReportResponse report =
                    reportParser.parse(requestId, records);

            validate(report, businessDate, currency);

            log.info(
                    "DAILY CLOSE [{}] completed successfully",
                    requestId
            );

            return report;
        } catch (TimeoutException exception) {
            throw new DailyCloseFailedException(
                    "Timed out waiting for daily close "
                            + businessDate + ".",
                    exception
            );
        } catch (DailyCloseFailedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DailyCloseFailedException(
                    "Daily close failed for " + businessDate + ".",
                    exception
            );
        } finally {
            tcpResultListener.unregisterDailyReport(requestId);
        }
    }

    private void validate(
            DailyCloseReportResponse report,
            LocalDate businessDate,
            String currency
    ) {
        if (!businessDate.equals(report.businessDate())) {
            throw new DailyCloseFailedException(
                    "Daily close returned date "
                            + report.businessDate()
                            + " instead of " + businessDate + "."
            );
        }

        if (!currency.equals(report.currency())) {
            throw new DailyCloseFailedException(
                    "Daily close returned currency "
                            + report.currency()
                            + " instead of " + currency + "."
            );
        }

        if (!"CLOSED".equals(report.state())
                || !"OK".equals(report.resultCode())) {
            throw new DailyCloseFailedException(
                    "Daily close did not return CLOSED/OK."
            );
        }
    }
}
