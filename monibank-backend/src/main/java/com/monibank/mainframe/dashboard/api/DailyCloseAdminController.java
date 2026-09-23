package com.monibank.mainframe.dashboard.api;

import com.monibank.mainframe.config.DailyCloseProperties;
import com.monibank.mainframe.dashboard.DailyCloseOrchestrator;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/daily-close")
@RequiredArgsConstructor
@Validated
public class DailyCloseAdminController {

    private final DailyCloseOrchestrator orchestrator;
    private final DailyCloseProperties properties;

    @PostMapping
    public ResponseEntity<DailyCloseReportResponse> closeDay(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            @Pattern(regexp = "[A-Z]{3}")
            String currency
    ) {
        LocalDate requestedDate = date == null
                ? LocalDate.now(properties.zoneId()).minusDays(1)
                : date;

        return ResponseEntity.ok(
                orchestrator.close(requestedDate, currency)
        );
    }

    @PostMapping("/report")
    public ResponseEntity<DailyCloseReportResponse> regenerateReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            @Pattern(regexp = "[A-Z]{3}")
            String currency
    ) {
        return ResponseEntity.ok(
                orchestrator.regenerateReport(date, currency)
        );
    }
}
