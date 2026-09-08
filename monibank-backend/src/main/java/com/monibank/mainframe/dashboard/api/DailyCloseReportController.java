package com.monibank.mainframe.dashboard.api;

import com.monibank.mainframe.dashboard.DailyCloseReportService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard/daily-close")
@RequiredArgsConstructor
@Validated
public class DailyCloseReportController {

    private final DailyCloseReportService dailyCloseReportService;

    @GetMapping
    public ResponseEntity<DailyCloseReportResponse> getDailyClose(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(defaultValue = "EUR")
            @Pattern(regexp = "[A-Z]{3}")
            String currency
    ) {

        LocalDate requestedDate =
                date == null
                        ? LocalDate.now().minusDays(1)
                        : date;

        return ResponseEntity.ok(
                dailyCloseReportService.getReport(
                        requestedDate,
                        currency
                )
        );
    }
}
