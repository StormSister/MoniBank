package com.monibank.mainframe.hercules;

import com.monibank.mainframe.model.MainframeResult;
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
public class MainframeResponseExecutor {

    private static final Duration TCP_TIMEOUT =
            Duration.ofSeconds(5);

    private static final int FALLBACK_ATTEMPTS = 10;

    private static final long FALLBACK_DELAY_MS = 500;

    private final MainframeResultStore mainframeResultStore;
    private final MainframeResultParser mainframeResultParser;
    private final MainframeTcpResultListener tcpResultListener;

    public MainframeResult execute(
            String requestId,
            String expectedOperation,
            String fallbackDataset,
            Runnable sendRequest
    ) {

        log.info(
                "MAINFRAME [{}] Registering result listener",
                requestId
        );

        tcpResultListener.register(requestId);

        try {

            /*
             * Request is sent only after the listener has been
             * registered. This prevents losing a fast response.
             */
            sendRequest.run();

            List<String> rawRecords =
                    receiveResult(
                            requestId,
                            fallbackDataset
                    );

            log.info(
                    "MAINFRAME [{}] Parsing {} result record(s)",
                    requestId,
                    rawRecords.size()
            );

            MainframeResult result =
                    mainframeResultParser.parse(
                            rawRecords
                    );

            log.info(
                    "MAINFRAME [{}] Parsed result - "
                            + "type={}, operation={}, code={}, "
                            + "entity={}, status={}, dataRecords={}",
                    requestId,
                    result.header().type(),
                    result.header().operation(),
                    result.header().code(),
                    result.header().entityId(),
                    result.header().status(),
                    result.data().size()
            );

            validateResult(
                    expectedOperation,
                    result
            );

            deleteFallbackDataset(
                    requestId,
                    fallbackDataset
            );

            return result;

        } finally {

            tcpResultListener.unregister(requestId);

            log.info(
                    "MAINFRAME [{}] Result listener unregistered",
                    requestId
            );
        }
    }

    private List<String> receiveResult(
            String requestId,
            String fallbackDataset
    ) {

        try {

            log.info(
                    "MAINFRAME [{}] Waiting for TCP result "
                            + "(timeout {}s)",
                    requestId,
                    TCP_TIMEOUT.toSeconds()
            );

            List<String> records =
                    tcpResultListener.await(
                            requestId,
                            TCP_TIMEOUT
                    );

            log.info(
                    "MAINFRAME [{}] TCP SUCCESS - "
                            + "received {} record(s)",
                    requestId,
                    records.size()
            );

            return records;

        } catch (TimeoutException exception) {

            if (!hasFallbackDataset(fallbackDataset)) {

                throw new IllegalStateException(
                        "TCP result timed out and no fallback "
                                + "dataset is available for request "
                                + requestId,
                        exception
                );
            }

            log.warn(
                    "MAINFRAME [{}] TCP TIMEOUT - "
                            + "switching to dataset fallback {}",
                    requestId,
                    fallbackDataset
            );

            return readResultWithRetry(
                    requestId,
                    fallbackDataset
            );
        }
    }

    private List<String> readResultWithRetry(
            String requestId,
            String fallbackDataset
    ) {

        Exception lastException = null;

        for (int attempt = 1;
             attempt <= FALLBACK_ATTEMPTS;
             attempt++) {

            try {

                log.info(
                        "MAINFRAME [{}] FALLBACK - "
                                + "dataset read attempt {}/{}",
                        requestId,
                        attempt,
                        FALLBACK_ATTEMPTS
                );

                List<String> records =
                        mainframeResultStore.read(
                                fallbackDataset
                        );

                log.info(
                        "MAINFRAME [{}] FALLBACK SUCCESS - "
                                + "received {} record(s) from {}",
                        requestId,
                        records.size(),
                        fallbackDataset
                );

                return records;

            } catch (Exception exception) {

                lastException = exception;

                log.warn(
                        "MAINFRAME [{}] FALLBACK - dataset {} "
                                + "not ready on attempt {}/{}: {}",
                        requestId,
                        fallbackDataset,
                        attempt,
                        FALLBACK_ATTEMPTS,
                        exception.getMessage()
                );

                sleep();
            }
        }

        throw new IllegalStateException(
                "Fallback dataset could not be read: "
                        + fallbackDataset,
                lastException
        );
    }

    private void validateResult(
            String expectedOperation,
            MainframeResult result
    ) {

        if ("E".equals(result.header().type())) {

            throw new IllegalStateException(
                    "Mainframe error: "
                            + result.header().code()
            );
        }

        if (!"S".equals(result.header().type())) {

            throw new IllegalStateException(
                    "Unexpected mainframe result type: "
                            + result.header().type()
            );
        }

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

    private void deleteFallbackDataset(
            String requestId,
            String fallbackDataset
    ) {

        if (!hasFallbackDataset(fallbackDataset)) {
            return;
        }

        mainframeResultStore.delete(
                fallbackDataset
        );

        log.info(
                "MAINFRAME [{}] Result dataset {} "
                        + "scheduled for deletion",
                requestId,
                fallbackDataset
        );
    }

    private boolean hasFallbackDataset(
            String fallbackDataset
    ) {
        return fallbackDataset != null
                && !fallbackDataset.isBlank();
    }

    private void sleep() {

        try {

            Thread.sleep(
                    FALLBACK_DELAY_MS
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting for "
                            + "mainframe result",
                    exception
            );
        }
    }
}