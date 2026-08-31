package com.monibank.mainframe.hercules;

import com.monibank.mainframe.hercules.terminal.KicksTerminalSessionManager;
import com.monibank.mainframe.hercules.terminal.MbgwRequest;
import com.monibank.mainframe.hercules.terminal.MbgwTerminalResponse;
import com.monibank.mainframe.model.MainframeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
@RequiredArgsConstructor
public class KicksMainframeOperationExecutor {

    private static final Duration TERMINAL_TIMEOUT =
            Duration.ofSeconds(30);

    private static final int MAX_INPUT_LENGTH = 512;

    private final MainframeRequestIdGenerator requestIdGenerator;
    private final ObjectProvider<KicksTerminalSessionManager>
            sessionManagerProvider;
    private final MainframeResponseExecutor responseExecutor;

    public MainframeResult execute(
            String operation,
            String input
    ) {

        validateRequest(
                operation,
                input
        );

        KicksTerminalSessionManager sessionManager =
                requireSessionManager();

        String requestId =
                requestIdGenerator.next();

        MbgwRequest request =
                new MbgwRequest(
                        operation,
                        requestId,
                        input
                );

        log.info(
                "KICKS [{}] {} started",
                requestId,
                operation
        );

        MainframeResult result =
                responseExecutor.execute(
                        requestId,
                        operation,
                        null,
                        () -> sendTerminalRequest(
                                sessionManager,
                                request
                        )
                );

        log.info(
                "KICKS [{}] {} completed successfully",
                requestId,
                operation
        );

        return result;
    }

    private KicksTerminalSessionManager requireSessionManager() {

        KicksTerminalSessionManager sessionManager =
                sessionManagerProvider.getIfAvailable();

        if (sessionManager == null) {

            throw new IllegalStateException(
                    "KICKS terminal integration is disabled."
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

            throw new IllegalStateException(
                    "KICKS terminal request timed out: "
                            + request.requestId(),
                    exception
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();
            future.cancel(false);

            throw new IllegalStateException(
                    "Interrupted while waiting for KICKS request "
                            + request.requestId(),
                    exception
            );

        } catch (ExecutionException exception) {

            throw new IllegalStateException(
                    "KICKS terminal request failed: "
                            + request.requestId(),
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
