package com.monibank.mainframe.hercules;

import com.monibank.mainframe.model.MainframeResult;
import com.monibank.mainframe.port.MainframeResultStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Component
public class MainframeResponseExecutor {

    private static final Logger log = LoggerFactory.getLogger(
            MainframeResponseExecutor.class
    );

    private static final Duration TCP_TIMEOUT =
            Duration.ofSeconds(5);

    private static final int FALLBACK_ATTEMPTS = 10;

    private static final long FALLBACK_DELAY_MS = 500;

    private final MainframeResultStore mainframeResultStore;
    private final MainframeResultParser mainframeResultParser;
    private final MainframeTcpResultListener tcpResultListener;

    public MainframeResponseExecutor(
            MainframeResultStore mainframeResultStore,
            MainframeResultParser mainframeResultParser,
            MainframeTcpResultListener tcpResultListener
    ) {
        this.mainframeResultStore = mainframeResultStore;
        this.mainframeResultParser = mainframeResultParser;
        this.tcpResultListener = tcpResultListener;
    }

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
             *
             * A terminal may fail while preparing its screen for the next
             * request after COBOL has already committed and emitted the
             * final correlated MBR record. Preserve that send failure, but
             * still inspect the result channel before deciding the business
             * outcome.
             */
            RuntimeException sendFailure = null;

            try {
                sendRequest.run();
            } catch (RuntimeException exception) {
                sendFailure = exception;
                log.warn(
                        "MAINFRAME [{}] Request channel failed; "
                                + "checking for an authoritative result: {}",
                        requestId,
                        exception.getMessage()
                );
            }

            List<String> rawRecords;

            try {
                rawRecords = receiveResult(
                            requestId,
                            expectedOperation,
                            fallbackDataset
                );
            } catch (RuntimeException resultFailure) {
                if (sendFailure != null) {
                    sendFailure.addSuppressed(resultFailure);
                    throw sendFailure;
                }
                throw resultFailure;
            }

            if (sendFailure != null) {
                log.warn(
                        "MAINFRAME [{}] Correlated result recovered after "
                                + "request-channel failure",
                        requestId
                );
            }

            log.info(
                    "MAINFRAME [{}] Parsing {} result record(s)",
                    requestId,
                    rawRecords.size()
            );

            MainframeResult result;

            try {
                result = mainframeResultParser.parse(rawRecords);
            } catch (RuntimeException exception) {
                throw MainframeTechnicalException.badGateway(
                        "MAINFRAME_PROTOCOL_ERROR",
                        requestId,
                        expectedOperation,
                        "The mainframe returned an invalid response.",
                        exception
                );
            }

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
                    requestId,
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
            String expectedOperation,
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

                throw MainframeTechnicalException.gatewayTimeout(
                        "MAINFRAME_RESULT_TIMEOUT",
                        requestId,
                        expectedOperation,
                        "The mainframe result timed out.",
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
                    expectedOperation,
                    fallbackDataset
            );
        } catch (RuntimeException exception) {
            throw MainframeTechnicalException.unavailable(
                    "MAINFRAME_RESULT_LISTENER_FAILURE",
                    requestId,
                    expectedOperation,
                    "The mainframe result connection failed.",
                    exception
            );
        }
    }

    private List<String> readResultWithRetry(
            String requestId,
            String expectedOperation,
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

        throw MainframeTechnicalException.unavailable(
                "MAINFRAME_RESULT_UNAVAILABLE",
                requestId,
                expectedOperation,
                "The mainframe result is temporarily unavailable.",
                lastException
        );
    }

    private void validateResult(
            String requestId,
            String expectedOperation,
            MainframeResult result
    ) {

        if (!"S".equals(result.header().type())
                && !"E".equals(result.header().type())) {

            throw MainframeTechnicalException.badGateway(
                    "MAINFRAME_PROTOCOL_ERROR",
                    requestId,
                    expectedOperation,
                    "The mainframe returned an unexpected result type.",
                    null
            );
        }

        if (!requestId.equals(result.header().requestId())) {
            throw MainframeTechnicalException.badGateway(
                    "MAINFRAME_RESPONSE_MISMATCH",
                    requestId,
                    expectedOperation,
                    "The mainframe response did not match the request.",
                    null
            );
        }

        if (!expectedOperation.equals(
                result.header().operation()
        )) {

            throw MainframeTechnicalException.badGateway(
                    "MAINFRAME_RESPONSE_MISMATCH",
                    requestId,
                    expectedOperation,
                    "The mainframe response did not match the operation.",
                    null
            );
        }

        if ("E".equals(result.header().type())) {
            throw new MainframeBusinessException(
                    result.header().code(),
                    requestId,
                    expectedOperation
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
