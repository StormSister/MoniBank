package com.monibank.mainframe;

import com.monibank.mainframe.dashboard.api.DailyCloseReportResponse;
import com.monibank.mainframe.dashboard.mainframe.DailyStatisticsResultParser;
import com.monibank.mainframe.model.MainframeDataRecord;
import com.monibank.mainframe.model.MainframeResult;
import com.monibank.mainframe.model.MainframeResultHeader;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyStatisticsResultParserTest {

    private final DailyStatisticsResultParser parser =
            new DailyStatisticsResultParser();

    @Test
    void parsesFixedWidthDailyStatistics() {
        String transactions = pad(
                "20260907"
                        + "EUR"
                        + "000000004"
                        + "000000002"
                        + "+0000000001100.00"
                        + "000000001"
                        + "+0000000000600.00"
                        + "000000001"
                        + "+0000000000000.07"
        );

        String customers = pad(
                "20260907"
                        + "ALL"
                        + "000000006"
                        + "000000005"
                        + "000000001"
                        + "000000000"
        );

        MainframeResult result = new MainframeResult(
                new MainframeResultHeader(
                        "S",
                        "DAYSTAT",
                        "OK",
                        "R1234567",
                        "C"
                ),
                List.of(
                        new MainframeDataRecord("DAYTXN", transactions),
                        new MainframeDataRecord("DAYCUST", customers)
                )
        );

        DailyCloseReportResponse report = parser.parse(result);

        assertThat(report.requestId()).isEqualTo("R1234567");
        assertThat(report.businessDate())
                .isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(report.currency()).isEqualTo("EUR");
        assertThat(report.state()).isEqualTo("CLOSED");
        assertThat(report.resultCode()).isEqualTo("OK");
        assertThat(report.transactions().operationCount()).isEqualTo(4);
        assertThat(report.transactions().depositAmount())
                .isEqualByComparingTo(new BigDecimal("1100.00"));
        assertThat(report.transactions().withdrawalAmount())
                .isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(report.transactions().interestCount()).isEqualTo(1);
        assertThat(report.transactions().interestAmount())
                .isEqualByComparingTo(new BigDecimal("0.07"));
        assertThat(report.customers().totalCount()).isEqualTo(6);
        assertThat(report.customers().activeCount()).isEqualTo(5);
        assertThat(report.customers().inactiveCount()).isEqualTo(1);
        assertThat(report.customers().newCount()).isZero();
    }

    private String pad(String value) {
        return value + " ".repeat(119 - value.length());
    }
}
