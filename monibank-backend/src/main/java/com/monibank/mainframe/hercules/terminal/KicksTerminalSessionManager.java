package com.monibank.mainframe.hercules.terminal;

import com.monibank.mainframe.config.KicksTerminalProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final KicksTerminalProperties properties;
    private final BlockingQueue<QueueEntry> queue =
            new LinkedBlockingQueue<>();

    private final ExecutorService worker =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "kicks-terminal-worker"
                );
                thread.setDaemon(false);
                return thread;
            });

    private final AtomicBoolean acceptingRequests =
            new AtomicBoolean(true);

    private volatile KicksTerminalSession session;
    private volatile Throwable terminalFailure;

    public KicksTerminalSessionManager(
            KicksTerminalProperties properties
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "properties cannot be null."
        );
    }

    @PostConstruct
    public void start() {
        log.info("Starting persistent KICKS terminal worker");
        worker.execute(this::runWorker);
    }

    public CompletableFuture<MbgwTerminalResponse> submit(
            MbgwRequest request
    ) {
        Objects.requireNonNull(request, "request cannot be null.");

        if (!acceptingRequests.get()) {
            return CompletableFuture.failedFuture(
                    unavailableException()
            );
        }

        CompletableFuture<MbgwTerminalResponse> result =
                new CompletableFuture<>();

        queue.add(new RequestEntry(request, result));

        /*
         * The manager may have been closed between the first check
         * and adding the request to the queue.
         */
        if (!acceptingRequests.get()
                && queue.removeIf(entry -> entry instanceof RequestEntry requestEntry
                && requestEntry.result() == result)) {
            result.completeExceptionally(unavailableException());
        }

        return result;
    }

    public TerminalSessionState state() {
        KicksTerminalSession currentSession = session;

        if (currentSession == null) {
            return terminalFailure == null
                    ? TerminalSessionState.DISCONNECTED
                    : TerminalSessionState.FAILED;
        }

        return currentSession.state();
    }

    public int queuedRequestCount() {
        return (int) queue.stream()
                .filter(RequestEntry.class::isInstance)
                .count();
    }

    private void runWorker() {
        try {
            session = new KicksTerminalSession(properties);
            session.open();

            log.info(
                    "Persistent KICKS terminal session is ready "
                            + "to accept MBGW requests"
            );

            processQueue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            terminalFailure = exception;
        } catch (Exception exception) {
            terminalFailure = exception;
            log.error(
                    "Persistent KICKS terminal worker failed",
                    exception
            );
        } finally {
            acceptingRequests.set(false);
            failQueuedRequests(unavailableException());
            closeSession();
        }
    }

    private void processQueue() throws Exception {
        while (!Thread.currentThread().isInterrupted()) {
            QueueEntry entry = queue.take();

            if (entry == StopEntry.INSTANCE) {
                return;
            }

            RequestEntry requestEntry = (RequestEntry) entry;

            if (requestEntry.result().isCancelled()) {
                continue;
            }

            executeRequest(requestEntry);
        }
    }

    private void executeRequest(
            RequestEntry entry
    ) throws Exception {
        try {
            MbgwTerminalResponse response =
                    session.execute(entry.request());

            entry.result().complete(response);
        } catch (Exception exception) {
            entry.result().completeExceptionally(exception);

            /*
             * KicksTerminalSession changes its state to FAILED
             * after an execution error. Continuing with the same
             * terminal would therefore be unsafe.
             */
            throw exception;
        }
    }

    private void failQueuedRequests(Throwable failure) {
        QueueEntry entry;

        while ((entry = queue.poll()) != null) {
            if (entry instanceof RequestEntry requestEntry) {
                requestEntry.result()
                        .completeExceptionally(failure);
            }
        }
    }

    private void closeSession() {
        KicksTerminalSession currentSession = session;

        if (currentSession == null) {
            return;
        }

        try {
            currentSession.close();
            log.info("Persistent KICKS terminal session closed");
        } catch (Exception exception) {
            log.warn(
                    "Could not close persistent KICKS "
                            + "terminal session cleanly",
                    exception
            );
        }
    }

    private IllegalStateException unavailableException() {
        String message =
                "KICKS terminal manager is not accepting requests.";

        return terminalFailure == null
                ? new IllegalStateException(message)
                : new IllegalStateException(message, terminalFailure);
    }

    @Override
    @PreDestroy
    public synchronized void close() {
        if (!acceptingRequests.compareAndSet(true, false)) {
            return;
        }

        log.info("Stopping persistent KICKS terminal worker");
        queue.add(StopEntry.INSTANCE);
        worker.shutdown();

        try {
            if (!worker.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn(
                        "KICKS terminal worker did not stop "
                                + "within 30 seconds"
                );
                worker.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            worker.shutdownNow();
        }
    }

    private sealed interface QueueEntry
            permits RequestEntry, StopEntry {
    }

    private record RequestEntry(
            MbgwRequest request,
            CompletableFuture<MbgwTerminalResponse> result
    ) implements QueueEntry {

        private RequestEntry {
            Objects.requireNonNull(
                    request,
                    "request cannot be null."
            );
            Objects.requireNonNull(
                    result,
                    "result cannot be null."
            );
        }
    }

    private enum StopEntry implements QueueEntry {
        INSTANCE
    }
}