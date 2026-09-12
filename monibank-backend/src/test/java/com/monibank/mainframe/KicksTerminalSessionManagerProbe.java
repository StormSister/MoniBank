package com.monibank.mainframe;

import com.monibank.mainframe.config.KicksTerminalProperties;
import com.monibank.mainframe.config.KicksTerminalDefinition;
import com.monibank.mainframe.hercules.terminal.KicksTerminalSessionManager;
import com.monibank.mainframe.hercules.terminal.DefaultKicksTerminalSessionFactory;
import com.monibank.mainframe.hercules.terminal.MbgwRequest;
import com.monibank.mainframe.hercules.terminal.MbgwTerminalResponse;
import com.monibank.mainframe.hercules.terminal.MbgwTerminalStatus;
import com.monibank.mainframe.hercules.terminal.TerminalSessionState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KicksTerminalSessionManagerProbe {

    @Test
    void processesQueuedRequestsInPersistentSession()
            throws Exception {

        KicksTerminalProperties properties =
                createProperties();

        KicksTerminalSessionManager manager =
                new KicksTerminalSessionManager(
                        properties,
                        new DefaultKicksTerminalSessionFactory()
                );

        try {
            manager.start();

            awaitState(
                    manager,
                    TerminalSessionState.READY,
                    Duration.ofSeconds(60)
            );

            /*
             * Wysyłamy oba żądania od razu.
             * Manager ma umieścić je w kolejce i wykonać kolejno
             * na jednym terminalu.
             */
            CompletableFuture<MbgwTerminalResponse> successFuture =
                    manager.submit(
                            new MbgwRequest(
                                    "GETCUST",
                                    "TEST0001",
                                    "C000000000006"
                            )
                    );

            CompletableFuture<MbgwTerminalResponse> notFoundFuture =
                    manager.submit(
                            new MbgwRequest(
                                    "GETCUST",
                                    "TEST0002",
                                    "C000000000001"
                            )
                    );

            MbgwTerminalResponse success =
                    successFuture.get(30, TimeUnit.SECONDS);

            MbgwTerminalResponse notFound =
                    notFoundFuture.get(30, TimeUnit.SECONDS);

            assertEquals(
                    MbgwTerminalStatus.SUCCESS,
                    success.status()
            );
            assertTrue(success.successful());
            assertTrue(
                    contains(success, "C000000000006;A;OK")
            );

            assertEquals(
                    MbgwTerminalStatus.ERROR,
                    notFound.status()
            );
            assertTrue(
                    contains(notFound, "NOTFOUND")
            );

            assertEquals(
                    TerminalSessionState.READY,
                    manager.state()
            );
            assertEquals(0, manager.queuedRequestCount());
        } finally {
            manager.close();
        }
    }

    private static KicksTerminalProperties createProperties() {
        String password = System.getenv("MB_TSO_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Missing MB_TSO_PASSWORD."
            );
        }

        String username = System.getenv("MB_TSO_USERNAME");

        if (username == null || username.isBlank()) {
            username = "HERC01";
        }

        String kicksCommand =
                System.getenv("MB_KICKS_STARTUP_COMMAND");

        if (kicksCommand == null || kicksCommand.isBlank()) {
            kicksCommand =
                    "EXEC '" + username + ".CMDPROC(MBKICKS)'";
        }

        return new KicksTerminalProperties(
                true,
                "127.0.0.1",
                13271,
                Duration.ofSeconds(2),
                1,
                List.of(
                        new KicksTerminalDefinition(
                                "TERM-1",
                                13270,
                                username,
                                password,
                                kicksCommand
                        )
                )
        );
    }

    private static void awaitState(
            KicksTerminalSessionManager manager,
            TerminalSessionState expected,
            Duration timeout
    ) throws InterruptedException {

        Instant deadline = Instant.now().plus(timeout);

        while (Instant.now().isBefore(deadline)) {
            TerminalSessionState current = manager.state();

            if (current == expected) {
                return;
            }

            if (current == TerminalSessionState.FAILED) {
                throw new IllegalStateException(
                        "Terminal manager entered FAILED state."
                );
            }

            Thread.sleep(100);
        }

        throw new IllegalStateException(
                "Terminal manager did not reach "
                        + expected
                        + " within "
                        + timeout.toSeconds()
                        + " seconds. Current state: "
                        + manager.state()
        );
    }

    private static boolean contains(
            MbgwTerminalResponse response,
            String expected
    ) {
        return response.screen().stream()
                .anyMatch(line -> line.contains(expected));
    }
}
