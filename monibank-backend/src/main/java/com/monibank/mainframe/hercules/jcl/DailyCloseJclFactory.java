package com.monibank.mainframe.hercules.jcl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class DailyCloseJclFactory {

    private static final DateTimeFormatter BUSINESS_DATE_FORMAT =
            DateTimeFormatter.BASIC_ISO_DATE;

    private static final DateTimeFormatter MEMBER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyMMdd");

    private final ResourceJclLoader resourceJclLoader;

    public String create(
            String requestId,
            LocalDate businessDate,
            String currency
    ) {
        if (requestId == null || !requestId.matches("R\\d{7}")) {
            throw new IllegalArgumentException(
                    "Request ID must match R followed by 7 digits."
            );
        }

        if (businessDate == null) {
            throw new IllegalArgumentException(
                    "Business date is required."
            );
        }

        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must contain three uppercase letters."
            );
        }

        String businessDateText =
                businessDate.format(BUSINESS_DATE_FORMAT);

        String reportMember =
                "D" + businessDate.format(MEMBER_DATE_FORMAT);

        String jcl = resourceJclLoader.load("MBCLSDY")
                .replace("${REQUEST_ID}", requestId)
                .replace("${BUSINESS_DATE}", businessDateText)
                .replace("${REPORT_MEMBER}", reportMember)
                .replace("${CURRENCY}", currency);

        if (jcl.contains("${")) {
            throw new IllegalStateException(
                    "MBCLSDY contains an unresolved placeholder."
            );
        }

        return jcl;
    }
}
