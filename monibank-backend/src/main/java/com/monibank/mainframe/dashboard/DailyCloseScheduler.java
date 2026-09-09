package com.monibank.mainframe.dashboard;

import com.monibank.mainframe.config.DailyCloseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class DailyCloseScheduler {

    private final DailyCloseProperties properties;
    private final DailyCloseOrchestrator orchestrator;
    private final DailyCloseReportService reportService;

    @Scheduled(
            cron = "${monibank.daily-close.cron:0 5 0 * * *}",
            zone = "${monibank.daily-close.zone:Europe/Warsaw}"
    )
    public void scheduledClose() {
        if (!properties.enabled()) {
            return;
        }

        runSafely(previousBusinessDate(), "scheduler");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpAfterStartup() {
        if (!properties.enabled()
                || !properties.catchUpOnStartup()) {
            return;
        }

        LocalDate date = previousBusinessDate();

        try {
            /*
             * First try to restore yesterday's already-created PDS member.
             * A backend restart must not repeat the close merely because the
             * in-memory cache is empty.
             */
            reportService.getReport(date, properties.currency());
            log.info(
                    "DAILY CLOSE startup restored report for {}",
                    date
            );
        } catch (DailyCloseReportUnavailableException exception) {
            runSafely(date, "startup catch-up");
        }
    }

    private LocalDate previousBusinessDate() {
        return LocalDate.now(properties.zoneId()).minusDays(1);
    }

    private void runSafely(
            LocalDate businessDate,
            String trigger
    ) {
        try {
            log.info(
                    "DAILY CLOSE {} triggered for {}",
                    trigger,
                    businessDate
            );

            orchestrator.close(
                    businessDate,
                    properties.currency()
            );
        } catch (Exception exception) {
            log.error(
                    "DAILY CLOSE {} failed for {}",
                    trigger,
                    businessDate,
                    exception
            );
        }
    }
}
