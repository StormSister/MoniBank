package com.monibank.mainframe.hercules.jcl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class DailyCloseReportJclFactory {

    private static final DateTimeFormatter BUSINESS_DATE_FORMAT =
            DateTimeFormatter.BASIC_ISO_DATE;

    private static final DateTimeFormatter MEMBER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyMMdd");

    private final ResourceJclLoader resourceJclLoader;

    public String create(
            String requestId,
            LocalDate businessDate
    ) {

        validateRequestId(requestId);

        if (businessDate == null) {
            throw new IllegalArgumentException(
                    "Business date is required."
            );
        }

        String reportMember =
                "D" + businessDate.format(MEMBER_DATE_FORMAT);

        String jcl =
                resourceJclLoader
                        .load("RPTREAD")
                        .replace(
                                "${REQUEST_ID}",
                                requestId
                        )
                        .replace(
                                "${BUSINESS_DATE}",
                                businessDate.format(
                                        BUSINESS_DATE_FORMAT
                                )
                        )
                        .replace(
                                "${REPORT_MEMBER}",
                                reportMember
                        );

        if (jcl.contains("${")) {
            throw new IllegalStateException(
                    "RPTREAD contains an unresolved placeholder."
            );
        }

        return jcl;
    }

    private void validateRequestId(String requestId) {

        if (requestId == null
                || !requestId.matches("R\\d{7}")) {
            throw new IllegalArgumentException(
                    "Request ID must match R followed by 7 digits."
            );
        }
    }
}
