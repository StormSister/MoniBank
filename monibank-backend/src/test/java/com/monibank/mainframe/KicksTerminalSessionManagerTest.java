package com.monibank.mainframe;

import com.monibank.mainframe.config.KicksTerminalDefinition;
import com.monibank.mainframe.config.KicksTerminalProperties;
import com.monibank.mainframe.hercules.terminal.KicksTerminalConnection;
import com.monibank.mainframe.hercules.terminal.KicksTerminalSessionManager;
import com.monibank.mainframe.hercules.terminal.MbgwRequest;
import com.monibank.mainframe.hercules.terminal.MbgwTerminalResponse;
import com.monibank.mainframe.hercules.terminal.MbgwTerminalStatus;
import com.monibank.mainframe.hercules.terminal.TerminalSessionState;
import com.monibank.mainframe.hercules.terminal.TsoUserInUseException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KicksTerminalSessionManagerTest {

    @Test
    void cancelsStuckConfiguredUserAndRebuildsSession()
            throws Exception {

        AtomicInteger createdSessions = new AtomicInteger();
        AtomicInteger cancellationRequests = new AtomicInteger();
        KicksTerminalProperties properties = properties(
                List.of(definition("TERM-1", 13270, "MBKSRV1"))
        );

        KicksTerminalSessionManager manager =
                new KicksTerminalSessionManager(
                        properties,
                        (ignoredProperties, ignoredDefinition) ->
                                createdSessions.incrementAndGet() == 1
                                        ? new InUseConnection("MBKSRV1")
                                        : new FakeConnection(false),
                        username -> {
                            assertEquals("MBKSRV1", username);
                            cancellationRequests.incrementAndGet();
                            return true;
                        }
                );

        try {
            manager.start();
            awaitState(manager, TerminalSessionState.READY);

            assertEquals(2, createdSessions.get());
            assertEquals(1, cancellationRequests.get());
            assertEquals(
                    1,
                    manager.terminalSnapshots().getFirst().recoveryCount()
            );
        } finally {
            manager.close();
        }
    }

    @Test
    void reusesHealthySessionForConsecutiveRequests()
            throws Exception {

        AtomicInteger createdSessions = new AtomicInteger();
        KicksTerminalProperties properties = properties(
                List.of(definition("TERM-1", 13270, "MBKSRV1"))
        );

        KicksTerminalSessionManager manager =
                new KicksTerminalSessionManager(
                        properties,
                        (ignoredProperties, ignoredDefinition) -> {
                            createdSessions.incrementAndGet();
                            return new FakeConnection(false);
                        }
                );

        try {
            manager.start();
            awaitState(manager, TerminalSessionState.READY);

            manager.submit(request("TEST0001"))
                    .get(2, TimeUnit.SECONDS);
            manager.submit(request("TEST0002"))
                    .get(2, TimeUnit.SECONDS);

            assertEquals(1, createdSessions.get());
            assertEquals(
                    TerminalSessionState.READY,
                    manager.state()
            );
        } finally {
            manager.close();
        }
    }

    @Test
    void rebuildsOnlyFailedTerminalAndAcceptsNextRequest()
            throws Exception {

        AtomicInteger createdSessions = new AtomicInteger();
        KicksTerminalProperties properties = properties(
                List.of(definition("TERM-1", 13270, "MBKSRV1"))
        );

        KicksTerminalSessionManager manager =
                new KicksTerminalSessionManager(
                        properties,
                        (ignoredProperties, ignoredDefinition) ->
                                new FakeConnection(
                                        createdSessions.incrementAndGet() == 1
                                )
                );

        try {
            manager.start();
            awaitState(manager, TerminalSessionState.READY);

            ExecutionException firstFailure = assertThrows(
                    ExecutionException.class,
                    () -> manager.submit(request("TEST0001"))
                            .get(2, TimeUnit.SECONDS)
            );
            assertEquals(
                    "simulated terminal failure",
                    firstFailure.getCause().getMessage()
            );

            MbgwTerminalResponse recovered =
                    manager.submit(request("TEST0002"))
                            .get(2, TimeUnit.SECONDS);

            assertEquals(MbgwTerminalStatus.SUCCESS, recovered.status());
            assertEquals("TEST0002", recovered.requestId());
            assertEquals(2, createdSessions.get());
            assertEquals(
                    1,
                    manager.terminalSnapshots().getFirst().recoveryCount()
            );
        } finally {
            manager.close();
        }
    }

    @Test
    void rejectsDuplicateUsersBeforeStartingWorkers() {
        KicksTerminalDefinition first =
                definition("TERM-1", 13270, "MBKSRV1");
        KicksTerminalDefinition second =
                definition("TERM-2", 13272, "MBKSRV1");

        KicksTerminalSessionManager manager =
                new KicksTerminalSessionManager(
                        properties(List.of(first, second)),
                        (ignoredProperties, ignoredDefinition) ->
                                new FakeConnection(false)
                );

        assertThrows(IllegalStateException.class, manager::start);
    }

    private static MbgwRequest request(String requestId) {
        return new MbgwRequest("GETCUST", requestId, "C000000000006");
    }

    private static KicksTerminalProperties properties(
            List<KicksTerminalDefinition> definitions
    ) {
        return new KicksTerminalProperties(
                true,
                "127.0.0.1",
                13271,
                Duration.ofMillis(10),
                definitions.size(),
                definitions
        );
    }

    private static KicksTerminalDefinition definition(
            String id,
            int port,
            String username
    ) {
        return new KicksTerminalDefinition(
                id,
                port,
                username,
                "MONIBANK",
                "EXEC 'HERC01.CMDPROC(MBKICKS)'"
        );
    }

    private static void awaitState(
            KicksTerminalSessionManager manager,
            TerminalSessionState expected
    ) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(2);

        while (Instant.now().isBefore(deadline)) {
            if (manager.state() == expected) {
                return;
            }
            Thread.sleep(10);
        }

        throw new IllegalStateException(
                "Manager did not reach " + expected
        );
    }

    private static final class FakeConnection
            implements KicksTerminalConnection {

        private final boolean failExecution;
        private TerminalSessionState state =
                TerminalSessionState.DISCONNECTED;

        private FakeConnection(boolean failExecution) {
            this.failExecution = failExecution;
        }

        @Override
        public TerminalSessionState state() {
            return state;
        }

        @Override
        public void open() {
            state = TerminalSessionState.READY;
        }

        @Override
        public void verifyReady() {
            if (state != TerminalSessionState.READY) {
                throw new IllegalStateException(
                        "simulated terminal is not ready"
                );
            }
        }

        @Override
        public MbgwTerminalResponse execute(MbgwRequest request) {
            state = TerminalSessionState.BUSY;

            if (failExecution) {
                state = TerminalSessionState.FAILED;
                throw new IllegalStateException(
                        "simulated terminal failure"
                );
            }

            state = TerminalSessionState.READY;
            return new MbgwTerminalResponse(
                    request.requestId(),
                    MbgwTerminalStatus.SUCCESS,
                    List.of("SUCCESS")
            );
        }

        @Override
        public void close() {
            state = TerminalSessionState.CLOSED;
        }
    }

    private static final class InUseConnection
            implements KicksTerminalConnection {

        private final String username;
        private TerminalSessionState state =
                TerminalSessionState.DISCONNECTED;

        private InUseConnection(String username) {
            this.username = username;
        }

        @Override
        public TerminalSessionState state() {
            return state;
        }

        @Override
        public void open() {
            state = TerminalSessionState.FAILED;
            throw new TsoUserInUseException(username);
        }

        @Override
        public void verifyReady() {
            throw new IllegalStateException("session is not ready");
        }

        @Override
        public MbgwTerminalResponse execute(MbgwRequest request) {
            throw new IllegalStateException("session is not ready");
        }

        @Override
        public void close() {
            state = TerminalSessionState.CLOSED;
        }
    }
}
