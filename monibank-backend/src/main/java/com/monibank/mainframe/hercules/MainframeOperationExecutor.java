package com.monibank.mainframe.hercules;

import com.monibank.mainframe.model.MainframeOperationSpec;
import com.monibank.mainframe.model.MainframeOperationType;
import com.monibank.mainframe.model.MainframeResult;
import com.monibank.mainframe.port.MainframeGateway;
import com.monibank.mainframe.port.MainframeResultStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
@RequiredArgsConstructor
public class MainframeOperationExecutor {

    private static final Duration TCP_TIMEOUT =
            Duration.ofSeconds(5);

    private static final int FALLBACK_ATTEMPTS = 10;

    private static final long FALLBACK_DELAY_MS = 500;

    private final MainframeJclFactory jclFactory;
    private final MainframeGateway mainframeGateway;
    private final JobNameGenerator jobNameGenerator;
    private final MainframeResultStore mainframeResultStore;
    private final MainframeResultParser mainframeResultParser;
    private final MainframeTcpResultListener tcpResultListener;

    public MainframeResult execute(
            String requestId,
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        String jobName =
                jobNameGenerator.next();

        String resultDataset =
                "MBANK.RES." + requestId;

        log.info(
                "MAINFRAME [{}] {} started - job={}, dataset={}",
                requestId,
                spec.operationName(),
                jobName,
                resultDataset
        );

        tcpResultListener.register(requestId);

        try {

            String jcl =
                    createJcl(
                            jobName,
                            resultDataset,
                            spec,
                            inputRecord
                    );

            mainframeGateway.submitJcl(jcl);

            log.info(
                    "MAINFRAME [{}] Job {} submitted",
                    requestId,
                    jobName
            );

            List<String> rawRecords =
                    receiveResult(
                            requestId,
                            resultDataset
                    );

            log.info(
                    "MAINFRAME [{}] Parsing result",
                    requestId
            );

            MainframeResult result =
                    mainframeResultParser.parse(
                            rawRecords
                    );

            log.info(
                    "MAINFRAME [{}] Parsed result - type={}, operation={}, code={}, entity={}, status={}, dataRecords={}",
                    requestId,
                    result.header().type(),
                    result.header().operation(),
                    result.header().code(),
                    result.header().entityId(),
                    result.header().status(),
                    result.data().size()
            );

            validateResult(
                    spec,
                    result
            );

            mainframeResultStore.delete(
                    resultDataset
            );

            log.info(
                    "MAINFRAME [{}] Result dataset {} scheduled for deletion",
                    requestId,
                    resultDataset
            );

            log.info(
                    "MAINFRAME [{}] {} COMPLETED SUCCESSFULLY",
                    requestId,
                    spec.operationName()
            );

            return result;

        } catch (Exception e) {

            log.error(
                    "MAINFRAME [{}] {} FAILED - job={}, dataset={}, error={}",
                    requestId,
                    spec.operationName(),
                    jobName,
                    resultDataset,
                    e.getMessage(),
                    e
            );

            throw e;

        } finally {

            tcpResultListener.unregister(
                    requestId
            );

            log.info(
                    "MAINFRAME [{}] TCP listener unregistered",
                    requestId
            );
        }
    }

    private String createJcl(
            String jobName,
            String resultDataset,
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        return switch (spec.type()) {

            case WRITE ->
                    jclFactory.create(
                            jobName,
                            resultDataset,
                            spec,
                            inputRecord
                    );

            case READ_ONE ->
                    jclFactory.createReadOne(
                            jobName,
                            resultDataset,
                            spec,
                            inputRecord
                    );

            case UPDATE ->
                    jclFactory.createUpdate(
                            jobName,
                            resultDataset,
                            spec,
                            inputRecord
                    );

            case READ_ALL ->
                    jclFactory.createReadAll(
                            jobName,
                            resultDataset,
                            spec,
                            inputRecord
                    );

        };
    }

    private List<String> receiveResult(
            String requestId,
            String resultDataset
    ) {

        try {

            log.info(
                    "MAINFRAME [{}] Waiting for TCP result (timeout {}s)",
                    requestId,
                    TCP_TIMEOUT.toSeconds()
            );

            List<String> records =
                    tcpResultListener.await(
                            requestId,
                            TCP_TIMEOUT
                    );

            log.info(
                    "MAINFRAME [{}] TCP SUCCESS - received {} record(s)",
                    requestId,
                    records.size()
            );

            return records;

        } catch (TimeoutException e) {

            log.warn(
                    "MAINFRAME [{}] TCP TIMEOUT - switching to DATASET FALLBACK",
                    requestId
            );

            return readResultWithRetry(
                    requestId,
                    resultDataset
            );
        }
    }

    private List<String> readResultWithRetry(
            String requestId,
            String resultDataset
    ) {

        Exception lastException = null;

        for (int attempt = 1;
             attempt <= FALLBACK_ATTEMPTS;
             attempt++) {

            try {

                log.info(
                        "MAINFRAME [{}] FALLBACK - dataset read attempt {}/{}",
                        requestId,
                        attempt,
                        FALLBACK_ATTEMPTS
                );

                List<String> records =
                        mainframeResultStore.read(
                                resultDataset
                        );

                log.info(
                        "MAINFRAME [{}] FALLBACK SUCCESS - received {} record(s) from dataset {}",
                        requestId,
                        records.size(),
                        resultDataset
                );

                return records;

            } catch (Exception e) {

                lastException = e;

                log.warn(
                        "MAINFRAME [{}] FALLBACK - dataset {} not ready on attempt {}/{}: {}",
                        requestId,
                        resultDataset,
                        attempt,
                        FALLBACK_ATTEMPTS,
                        e.getMessage()
                );

                sleep();
            }
        }

        throw new IllegalStateException(
                "Fallback dataset could not be read: "
                        + resultDataset,
                lastException
        );
    }

    private void validateResult(
            MainframeOperationSpec spec,
            MainframeResult result
    ) {

        if ("E".equals(
                result.header().type()
        )) {

            throw new IllegalStateException(
                    "Mainframe error: "
                            + result.header().code()
            );
        }

        if (!"S".equals(
                result.header().type()
        )) {

            throw new IllegalStateException(
                    "Unexpected mainframe result type: "
                            + result.header().type()
            );
        }

        String expectedOperation =
                expectedResultOperation(spec);

        if (!expectedOperation.equals(
                result.header().operation()
        )) {

            throw new IllegalStateException(
                    "Unexpected mainframe operation: "
                            + result.header().operation()
                            + ", expected "
                            + expectedOperation
            );
        }
    }

    private String expectedResultOperation(
            MainframeOperationSpec spec
    ) {

        /*
         * Na razie programName odpowiada nazwie
         * operacji zwracanej przez COBOL:
         *
         * CHGCUST -> CHGCUST
         * ADDCUST -> ADDCUST
         *
         * Jeżeli później będziemy mieli wyjątek,
         * dodamy resultOperation do OperationSpec.
         */
        if (spec.programName() == null
                || spec.programName().isBlank()) {

            throw new IllegalStateException(
                    "Operation has no result operation name: "
                            + spec.operationName()
            );
        }

        return spec.programName();
    }

    private void sleep() {

        try {

            Thread.sleep(
                    FALLBACK_DELAY_MS
            );

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting for mainframe result",
                    e
            );
        }
    }
}