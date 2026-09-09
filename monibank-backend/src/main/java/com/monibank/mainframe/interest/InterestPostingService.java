package com.monibank.mainframe.interest;

import com.monibank.mainframe.hercules.KicksMainframeOperationExecutor;
import com.monibank.mainframe.interest.mainframe.InterestMainframeOperations;
import com.monibank.mainframe.interest.mainframe.InterestRecordMapper;
import com.monibank.mainframe.model.MainframeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterestPostingService {

    private final InterestRecordMapper interestRecordMapper;
    private final KicksMainframeOperationExecutor operationExecutor;

    public InterestPostingResult postDailyInterest(
            LocalDate businessDate,
            String currency,
            int annualRateBasisPoints
    ) {
        String input = interestRecordMapper.toRecord(
                businessDate,
                currency,
                annualRateBasisPoints
        );

        MainframeResult result = operationExecutor.execute(
                InterestMainframeOperations.POST_INTEREST,
                input
        );

        long postedCount = parsePostedCount(
                result.header().entityId()
        );

        log.info(
                "Daily interest posted for {} {}: {} account(s)",
                businessDate,
                currency,
                postedCount
        );

        return new InterestPostingResult(
                businessDate,
                currency,
                annualRateBasisPoints,
                postedCount,
                result.header().status()
        );
    }

    private long parsePostedCount(String value) {
        if (value == null || !value.matches("\\d{13}")) {
            throw new IllegalStateException(
                    "POSTINT returned invalid posted-account count: "
                            + value
            );
        }

        return Long.parseLong(value);
    }
}
