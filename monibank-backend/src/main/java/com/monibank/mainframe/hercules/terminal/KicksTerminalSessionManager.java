package com.monibank.mainframe.hercules.terminal;

import com.monibank.mainframe.config.KicksTerminalDefinition;
import com.monibank.mainframe.config.KicksTerminalProperties;
import com.monibank.operations.LegacyOperationTracker;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConditionalOnProperty(
        prefix = "monibank.mainframe.terminal",
        name = "enabled",
        havingValue = "true"
)
public final class KicksTerminalSessionManager
        implements AutoCloseable {

    private static final Logger log =
            LoggerFactory.getLogger(KicksTerminalSessionManager.class);
    private static final Duration DEFAULT_RECOVERY_DELAY =
            Duration.ofSeconds(2);
    private static final Duration HEALTH_CHECK_INTERVAL =
            Duration.ofSeconds(15);
    private static final Duration TSO_CANCEL_COOLDOWN =
            Duration.ofSeconds(30);

    private final KicksTerminalProperties properties;
    private final KicksTerminalSessionFactory sessionFactory;
    private final TsoSessionRecovery tsoSessionRecovery;
    private final LegacyOperationTracker operationTracker;
    private final BlockingQueue<QueueEntry> queue =
            new LinkedBlockingQueue<>();
    private final List<TerminalWorker> terminalWorkers =
            new CopyOnWriteArrayList<>();
    private final AtomicBoolean acceptingRequests =
            new AtomicBoolean(false);

    private ExecutorService workerPool;

    public KicksTerminalSessionManager(
            KicksTerminalProperties properties,
            KicksTerminalSessionFactory sessionFactory
    ) {
        this(
                properties,
                sessionFactory,
                username -> false,
                LegacyOperationTracker.noop()
        );
    }

    public KicksTerminalSessionManager(
            KicksTerminalProperties properties,
            KicksTerminalSessionFactory sessionFactory,
            TsoSessionRecovery tsoSessionRecovery
    ) {
        this(
                properties,
                sessionFactory,
                tsoSessionRecovery,
                LegacyOperationTracker.noop()
        );
    }

    @Autowired
    public KicksTerminalSessionManager(
            KicksTerminalProperties properties,
            KicksTerminalSessionFactory sessionFactory,
            TsoSessionRecovery tsoSessionRecovery,
            LegacyOperationTracker operationTracker
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "properties cannot be null."
        );
        this.sessionFactory = Objects.requireNonNull(
                sessionFactory,
                "sessionFactory cannot be null."
        );
        this.tsoSessionRecovery = Objects.requireNonNull(
                tsoSessionRecovery,
                "tsoSessionRecovery cannot be null."
        );
        this.operationTracker = Objects.requireNonNull(
                operationTracker,
                "operationTracker cannot be null."
        );
    }

    @PostConstruct
    public synchronized void start() {
        if (acceptingRequests.get()) {
            return;
        }

        List<KicksTerminalDefinition> definitions =
                validateAndCopyDefinitions();
        AtomicInteger threadNumber = new AtomicInteger();

        workerPool = Executors.newFixedThreadPool(
                definitions.size(),
                runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "kicks-terminal-worker-"
                                    + threadNumber.incrementAndGet()
                    );
                    thread.setDaemon(false);
                    return thread;
                }
        );

        acceptingRequests.set(true);
        log.info(
                "Starting KICKS terminal pool with {} session(s)",
                definitions.size()
        );

        for (KicksTerminalDefinition definition : definitions) {
            TerminalWorker worker = new TerminalWorker(definition);
            terminalWorkers.add(worker);
            workerPool.execute(worker);
        }
    }

    public CompletableFuture<MbgwTerminalResponse> submit(
            MbgwRequest request
    ) {
        Objects.requireNonNull(request, "request cannot be null.");

        if (!acceptingRequests.get()) {
            return CompletableFuture.failedFuture(unavailableException());
        }

        CompletableFuture<MbgwTerminalResponse> result =
                new CompletableFuture<>();
        RequestEntry entry = new RequestEntry(
                request,
                result,
                Instant.now()
        );
        queue.add(entry);

        if (!acceptingRequests.get() && queue.remove(entry)) {
            result.completeExceptionally(unavailableException());
        }

        return result;
    }

    public TerminalSessionState state() {
        List<TerminalSessionSnapshot> snapshots = terminalSnapshots();

        if (hasState(snapshots, TerminalSessionState.READY)) {
            return TerminalSessionState.READY;
        }
        if (hasState(snapshots, TerminalSessionState.BUSY)) {
            return TerminalSessionState.BUSY;
        }
        if (hasState(snapshots, TerminalSessionState.RECOVERING)) {
            return TerminalSessionState.RECOVERING;
        }
        if (snapshots.stream().anyMatch(snapshot ->
                snapshot.state() == TerminalSessionState.CONNECTING
                        || snapshot.state() == TerminalSessionState.LOGGING_IN
                        || snapshot.state() == TerminalSessionState.STARTING_KICKS
                        || snapshot.state() == TerminalSessionState.OPENING_MBGW
        )) {
            return TerminalSessionState.CONNECTING;
        }
        if (snapshots.isEmpty()) {
            return TerminalSessionState.DISCONNECTED;
        }
        return snapshots.stream().allMatch(snapshot ->
                snapshot.state() == TerminalSessionState.CLOSED
        ) ? TerminalSessionState.CLOSED : TerminalSessionState.FAILED;
    }

    public List<TerminalSessionSnapshot> terminalSnapshots() {
        return terminalWorkers.stream()
                .map(TerminalWorker::snapshot)
                .toList();
    }

    public int queuedRequestCount() {
        return (int) queue.stream()
                .filter(RequestEntry.class::isInstance)
                .map(RequestEntry.class::cast)
                .filter(entry -> !entry.result().isDone())
                .count();
    }

    private List<KicksTerminalDefinition> validateAndCopyDefinitions() {
        requireText("host", properties.host());
        requirePort("port", properties.port());

        List<KicksTerminalDefinition> definitions = properties.sessions();
        if (definitions == null || definitions.isEmpty()) {
            throw new IllegalStateException(
                    "At least one KICKS terminal session must be configured."
            );
        }

        if (properties.poolSize() < 1
                || properties.poolSize() > definitions.size()) {
            throw new IllegalStateException(
                    "Terminal pool size must be between 1 and "
                            + definitions.size()
                            + ", but was " + properties.poolSize() + "."
            );
        }

        definitions = definitions.subList(0, properties.poolSize());

        Set<String> ids = new HashSet<>();
        Set<String> usernames = new HashSet<>();
        Set<Integer> controlPorts = new HashSet<>();

        for (KicksTerminalDefinition definition : definitions) {
            if (definition == null) {
                throw new IllegalStateException(
                        "KICKS terminal session cannot be null."
                );
            }

            requireText("session.id", definition.id());
            requireText("session.username", definition.username());
            requireText("session.password", definition.password());
            requireText(
                    "session.kicksStartupCommand",
                    definition.kicksStartupCommand()
            );
            requirePort(
                    "session.emulatorControlPort",
                    definition.emulatorControlPort()
            );

            if (!ids.add(definition.id())) {
                throw new IllegalStateException(
                        "Duplicate KICKS terminal id: " + definition.id()
                );
            }
            if (!usernames.add(definition.username())) {
                throw new IllegalStateException(
                        "Each KICKS terminal requires a separate TSO user. "
                                + "Duplicate user: " + definition.username()
                );
            }
            if (!controlPorts.add(definition.emulatorControlPort())) {
                throw new IllegalStateException(
                        "Duplicate emulator control port: "
                                + definition.emulatorControlPort()
                );
            }
        }

        return List.copyOf(definitions);
    }

    private Duration recoveryDelay() {
        Duration configured = properties.recoveryDelay();
        return configured == null || configured.isNegative()
                || configured.isZero()
                ? DEFAULT_RECOVERY_DELAY
                : configured;
    }

    private void failQueuedRequests(Throwable failure) {
        QueueEntry entry;
        while ((entry = queue.poll()) != null) {
            if (entry instanceof RequestEntry requestEntry) {
                requestEntry.result().completeExceptionally(failure);
            }
        }
    }

    private IllegalStateException unavailableException() {
        return new IllegalStateException(
                "KICKS terminal pool is not accepting requests."
        );
    }

    @Override
    @PreDestroy
    public synchronized void close() {
        if (!acceptingRequests.compareAndSet(true, false)) {
            return;
        }

        log.info("Stopping KICKS terminal pool");
        failQueuedRequests(unavailableException());

        if (workerPool == null) {
            return;
        }

        for (int index = 0; index < terminalWorkers.size(); index++) {
            queue.add(StopEntry.INSTANCE);
        }
        workerPool.shutdown();

        try {
            if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn(
                        "KICKS terminal pool did not stop within 30 seconds"
                );
                workerPool.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private final class TerminalWorker implements Runnable {

        private final KicksTerminalDefinition definition;

        private volatile KicksTerminalConnection session;
        private volatile TerminalSessionState workerState =
                TerminalSessionState.DISCONNECTED;
        private volatile String currentRequestId;
        private volatile long recoveryCount;
        private volatile Instant readySince;
        private volatile Instant lastFailureAt;
        private volatile String lastError;
        private volatile Instant lastTsoCancelAttempt;

        private TerminalWorker(KicksTerminalDefinition definition) {
            this.definition = definition;
        }

        @Override
        public void run() {
            Thread.currentThread().setName(
                    "kicks-terminal-" + definition.id()
            );

            try {
                while (acceptingRequests.get()
                        && !Thread.currentThread().isInterrupted()) {
                    if (!ensureReadySession()) {
                        return;
                    }

                    QueueEntry queueEntry = queue.poll(
                            HEALTH_CHECK_INTERVAL.toMillis(),
                            TimeUnit.MILLISECONDS
                    );

                    if (queueEntry == null) {
                        verifyIdleSession();
                        continue;
                    }

                    if (queueEntry == StopEntry.INSTANCE) {
                        return;
                    }

                    RequestEntry entry = (RequestEntry) queueEntry;
                    if (entry.result().isCancelled()) {
                        continue;
                    }

                    execute(entry);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                closeCurrentSession();
                workerState = TerminalSessionState.CLOSED;
                currentRequestId = null;
                log.info("KICKS terminal {} stopped", definition.id());
            }
        }

        private boolean ensureReadySession()
                throws InterruptedException {

            KicksTerminalConnection current = session;

            if (current != null
                    && current.state() == TerminalSessionState.READY) {
                workerState = TerminalSessionState.READY;
                return true;
            }

            while (acceptingRequests.get()
                    && !Thread.currentThread().isInterrupted()) {
                workerState = recoveryCount == 0
                        ? TerminalSessionState.CONNECTING
                        : TerminalSessionState.RECOVERING;

                KicksTerminalConnection candidate =
                        sessionFactory.create(properties, definition);
                session = candidate;

                try {
                    log.info(
                            "KICKS terminal {} connecting as {}",
                            definition.id(),
                            definition.username()
                    );
                    candidate.open();
                    readySince = Instant.now();
                    workerState = TerminalSessionState.READY;
                    log.info("KICKS terminal {} is READY", definition.id());
                    return true;
                } catch (Exception exception) {
                    if (exception instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        return false;
                    }

                    recordFailure(exception);
                    closeCurrentSession();
                    boolean cancellationRequested =
                            requestCancellationIfUserInUse(exception);
                    Duration delay = retryDelay(
                            exception,
                            cancellationRequested
                    );
                    workerState = TerminalSessionState.RECOVERING;
                    log.warn(
                            "KICKS terminal {} startup failed; retrying in {} ms: {}",
                            definition.id(),
                            delay.toMillis(),
                            exception.getMessage()
                    );
                    Thread.sleep(delay.toMillis());
                }
            }

            return false;
        }

        private boolean requestCancellationIfUserInUse(
                Exception failure
        ) {
            if (!(failure instanceof TsoUserInUseException)) {
                return false;
            }

            Instant now = Instant.now();
            Instant previousAttempt = lastTsoCancelAttempt;

            if (previousAttempt != null
                    && previousAttempt.plus(TSO_CANCEL_COOLDOWN)
                    .isAfter(now)) {
                return false;
            }

            lastTsoCancelAttempt = now;

            try {
                return tsoSessionRecovery.cancel(
                        definition.username()
                );
            } catch (RuntimeException exception) {
                lastError = "TSO cancellation failed: "
                        + conciseMessage(exception);
                log.warn(
                        "KICKS terminal {} could not cancel stuck TSO user {}",
                        definition.id(),
                        definition.username(),
                        exception
                );
                return false;
            }
        }

        private Duration retryDelay(
                Exception failure,
                boolean cancellationRequested
        ) {
            if (failure instanceof TsoUserInUseException
                    && !cancellationRequested) {
                return TSO_CANCEL_COOLDOWN;
            }

            return recoveryDelay();
        }

        private void verifyIdleSession() {
            try {
                session.verifyReady();
            } catch (Exception exception) {
                recordFailure(exception);
                log.warn(
                        "KICKS terminal {} failed idle health check; rebuilding session",
                        definition.id(),
                        exception
                );
                closeCurrentSession();
                workerState = TerminalSessionState.RECOVERING;
            }
        }

        private void execute(RequestEntry entry) {
            currentRequestId = entry.request().requestId();
            Instant startedAt = Instant.now();
            long queueDurationMs = elapsedMillis(
                    entry.queuedAt(),
                    startedAt
            );

            log.info(
                    "MBOP event=ASSIGNED requestId={} operation={} worker={} username={} queueMs={}",
                    currentRequestId,
                    entry.request().operation(),
                    definition.id(),
                    definition.username(),
                    queueDurationMs
            );
            operationTracker.assigned(
                    currentRequestId,
                    definition.id(),
                    definition.username(),
                    queueDurationMs
            );

            try {
                MbgwTerminalResponse response =
                        session.execute(entry.request());
                entry.result().complete(response);
                workerState = TerminalSessionState.READY;

                log.info(
                        "MBOP event=TERMINAL_COMPLETED requestId={} operation={} worker={} username={} terminalStatus={} queueMs={} durationMs={}",
                        currentRequestId,
                        entry.request().operation(),
                        definition.id(),
                        definition.username(),
                        response.status(),
                        queueDurationMs,
                        elapsedMillis(startedAt, Instant.now())
                );
            } catch (Exception exception) {
                entry.result().completeExceptionally(exception);
                recordFailure(exception);
                log.warn(
                        "MBOP event=TERMINAL_FAILED requestId={} operation={} worker={} username={} errorType={} queueMs={} durationMs={}",
                        currentRequestId,
                        entry.request().operation(),
                        definition.id(),
                        definition.username(),
                        exception.getClass().getSimpleName(),
                        queueDurationMs,
                        elapsedMillis(startedAt, Instant.now())
                );
                log.warn(
                        "KICKS terminal {} failed request {}; rebuilding session",
                        definition.id(),
                        currentRequestId,
                        exception
                );
                closeCurrentSession();
                workerState = TerminalSessionState.RECOVERING;
            } finally {
                currentRequestId = null;
            }
        }

        private void recordFailure(Exception exception) {
            recoveryCount++;
            readySince = null;
            lastFailureAt = Instant.now();
            lastError = conciseMessage(exception);
        }

        private void closeCurrentSession() {
            KicksTerminalConnection current = session;
            session = null;

            if (current == null) {
                return;
            }

            try {
                current.close();
            } catch (Exception exception) {
                lastFailureAt = Instant.now();
                lastError = "Cleanup failed: " + conciseMessage(exception);
                log.warn(
                        "KICKS terminal {} could not log off cleanly",
                        definition.id(),
                        exception
                );
            }
        }

        private TerminalSessionSnapshot snapshot() {
            KicksTerminalConnection current = session;
            TerminalSessionState currentState = current == null
                    ? workerState
                    : current.state();

            if (currentState == TerminalSessionState.FAILED
                    && workerState == TerminalSessionState.RECOVERING) {
                currentState = TerminalSessionState.RECOVERING;
            }

            return new TerminalSessionSnapshot(
                    definition.id(),
                    definition.username(),
                    currentState,
                    currentRequestId,
                    recoveryCount,
                    readySince,
                    lastFailureAt,
                    lastError
            );
        }
    }

    private static boolean hasState(
            List<TerminalSessionSnapshot> snapshots,
            TerminalSessionState state
    ) {
        return snapshots.stream().anyMatch(
                snapshot -> snapshot.state() == state
        );
    }

    private static String conciseMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : message;
    }

    private static long elapsedMillis(
            Instant startedAt,
            Instant finishedAt
    ) {
        return Math.max(
                0L,
                Duration.between(startedAt, finishedAt).toMillis()
        );
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing terminal configuration: " + name
            );
        }
    }

    private static void requirePort(String name, int value) {
        if (value < 1 || value > 65535) {
            throw new IllegalStateException(
                    "Invalid terminal port " + name + ": " + value
            );
        }
    }

    private sealed interface QueueEntry
            permits RequestEntry, StopEntry {
    }

    private record RequestEntry(
            MbgwRequest request,
            CompletableFuture<MbgwTerminalResponse> result,
            Instant queuedAt
    ) implements QueueEntry {
        private RequestEntry {
            Objects.requireNonNull(request, "request cannot be null.");
            Objects.requireNonNull(result, "result cannot be null.");
            Objects.requireNonNull(queuedAt, "queuedAt cannot be null.");
        }
    }

    private enum StopEntry implements QueueEntry {
        INSTANCE
    }
}
