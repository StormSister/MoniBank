package com.monibank.mainframe.hercules;

import com.monibank.mainframe.hercules.terminal.KicksTerminalSessionManager;
import com.monibank.mainframe.hercules.terminal.MbgwRequest;
import com.monibank.mainframe.hercules.terminal.MbgwTerminalResponse;
import com.monibank.mainframe.model.MainframeResult;
import com.monibank.operations.LegacyOperationTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KicksMainframeOperationExecutor {

    private static final Logger log = LoggerFactory.getLogger(
            KicksMainframeOperationExecutor.class
    );

    private static final Duration TERMINAL_TIMEOUT =
            Duration.ofSeconds(30);

    private static final int MAX_INPUT_LENGTH = 512;

    private final MainframeRequestIdGenerator requestIdGenerator;
    private final ObjectProvider<KicksTerminalSessionManager>
            sessionManagerProvider;
    private final MainframeResponseExecutor responseExecutor;
    private final LegacyOperationTracker operationTracker;

    public KicksMainframeOperationExecutor(
            MainframeRequestIdGenerator requestIdGenerator,
            ObjectProvider<KicksTerminalSessionManager> sessionManagerProvider,
            MainframeResponseExecutor responseExecutor,
            LegacyOperationTracker operationTracker
    ) {
        this.requestIdGenerator = requestIdGenerator;
        this.sessionManagerProvider = sessionManagerProvider;
        this.responseExecutor = responseExecutor;
        this.operationTracker = operationTracker;
    }

    public MainframeResult execute(
            String operation,
            String input
    ) {

        validateRequest(
                operation,
                input
        );

        String requestId =
                requestIdGenerator.next();
        operationTracker.started(requestId, operation);

        try {
            KicksTerminalSessionManager sessionManager =
                    requireSessionManager(requestId, operation);

            MbgwRequest request = new MbgwRequest(
                    operation,
                    requestId,
                    input
            );

            log.info(
                    "KICKS [{}] {} started",
                    requestId,
                    operation
            );

            MainframeResult result = responseExecutor.execute(
                    requestId,
                    operation,
                    null,
                    () -> sendTerminalRequest(sessionManager, request)
            );

            operationTracker.succeeded(
                    requestId,
                    result.header().code()
            );
            log.info(
                    "KICKS [{}] {} completed successfully",
                    requestId,
                    operation
            );
            return result;
        } catch (MainframeBusinessException exception) {
            operationTracker.businessFailed(
                    requestId,
                    exception.code()
            );
            throw exception;
        } catch (MainframeTechnicalException exception) {
            operationTracker.technicalFailed(
                    requestId,
                    exception.code(),
                    exception.getClass().getSimpleName(),
                    exception.retryable()
            );
            throw exception;
        } catch (RuntimeException exception) {
            operationTracker.technicalFailed(
                    requestId,
                    "UNEXPECTED_ERROR",
                    exception.getClass().getSimpleName(),
                    false
            );
            throw exception;
        }
    }

    private KicksTerminalSessionManager requireSessionManager(
            String requestId,
            String operation
    ) {

        KicksTerminalSessionManager sessionManager =
                sessionManagerProvider.getIfAvailable();

        if (sessionManager == null) {

            throw MainframeTechnicalException.unavailable(
                    "TERMINAL_INTEGRATION_DISABLED",
                    requestId,
                    operation,
                    "The terminal service is unavailable.",
                    null
            );
        }

        return sessionManager;
    }

    private void sendTerminalRequest(
            KicksTerminalSessionManager sessionManager,
            MbgwRequest request
    ) {

        CompletableFuture<MbgwTerminalResponse> future =
                sessionManager.submit(request);

        try {

            MbgwTerminalResponse response =
                    future.get(
                            TERMINAL_TIMEOUT.toSeconds(),
                            TimeUnit.SECONDS
                    );

            /*
             * ERROR is not rejected here. It may represent a
             * valid business error such as NOTFOUND. The final
             * MBR;E record will be handled by the common parser.
             */
            log.info(
                    "KICKS [{}] MBGW response: {}",
                    request.requestId(),
                    response
            );

        } catch (TimeoutException exception) {

            /*
             * If the request is still waiting in the queue,
             * cancellation lets the manager skip it.
             */
            future.cancel(false);

            throw MainframeTechnicalException.gatewayTimeout(
                    "TERMINAL_TIMEOUT",
                    request.requestId(),
                    request.operation(),
                    "No terminal completed the request in time.",
                    exception
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();
            future.cancel(false);

            throw MainframeTechnicalException.unavailable(
                    "TERMINAL_INTERRUPTED",
                    request.requestId(),
                    request.operation(),
                    "The terminal request was interrupted.",
                    exception
            );

        } catch (ExecutionException exception) {

            throw MainframeTechnicalException.unavailable(
                    "TERMINAL_FAILURE",
                    request.requestId(),
                    request.operation(),
                    "The terminal session failed while processing the request.",
                    exception.getCause()
            );
        }
    }

    private void validateRequest(
            String operation,
            String input
    ) {

        if (operation == null
                || operation.isBlank()) {

            throw new IllegalArgumentException(
                    "KICKS operation cannot be blank."
            );
        }

        if (operation.length() > 8) {

            throw new IllegalArgumentException(
                    "KICKS operation cannot exceed 8 characters."
            );
        }

        if (input == null) {

            throw new IllegalArgumentException(
                    "KICKS input cannot be null."
            );
        }

        if (input.length() > MAX_INPUT_LENGTH) {

            throw new IllegalArgumentException(
                    "KICKS input cannot exceed "
                            + MAX_INPUT_LENGTH
                            + " characters."
            );
        }
    }
}
