package com.monibank.operations;

import com.monibank.mainframe.operations.CoreOperationEvent;
import com.monibank.mainframe.operations.OperationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public final class LegacyOperationTracker {

    private static final Logger log = LoggerFactory.getLogger(
            LegacyOperationTracker.class
    );

    private final OperationEventPublisher publisher;
    private final ConcurrentMap<String, Context> activeOperations =
            new ConcurrentHashMap<>();

    public LegacyOperationTracker(OperationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(
                publisher,
                "publisher cannot be null."
        );
    }

    public static LegacyOperationTracker noop() {
        return new LegacyOperationTracker(ignored -> { });
    }

    public void started(String requestId, String operation) {
        Context context = new Context(
                normalize(requestId),
                normalize(operation),
                Instant.now()
        );
        activeOperations.put(context.requestId, context);
    }

    public void assigned(
            String requestId,
            String executorId,
            String username,
            long queueMs
    ) {
        Context context = activeOperations.get(normalize(requestId));
        if (context != null) {
            context.assign(
                    normalize(executorId),
                    normalize(username),
                    Math.max(0L, queueMs),
                    Instant.now()
            );
        }
    }

    public void succeeded(String requestId, String resultCode) {
        complete(
                requestId,
                CoreOperationEvent.Status.SUCCESS,
                resultCode,
                null,
                null
        );
    }

    public void businessFailed(String requestId, String resultCode) {
        complete(
                requestId,
                CoreOperationEvent.Status.BUSINESS_ERROR,
                resultCode,
                null,
                false
        );
    }

    public void technicalFailed(
            String requestId,
            String resultCode,
            String errorType,
            boolean retryable
    ) {
        complete(
                requestId,
                CoreOperationEvent.Status.TECHNICAL_ERROR,
                resultCode,
                errorType,
                retryable
        );
    }

    private void complete(
            String requestId,
            CoreOperationEvent.Status status,
            String resultCode,
            String errorType,
            Boolean retryable
    ) {
        String normalizedRequestId = normalize(requestId);
        Context context = activeOperations.remove(normalizedRequestId);

        if (context == null) {
            log.warn(
                    "No operation context found for request {}",
                    normalizedRequestId
            );
            return;
        }

        Instant completedAt = Instant.now();
        CoreOperationEvent event = context.toEvent(
                completedAt,
                status,
                normalizeNullable(resultCode),
                normalizeNullable(errorType),
                retryable
        );
        publisher.publish(event);

        log.info(
                "MBOP event=FINAL requestId={} core={} operation={} worker={} status={} resultCode={} queueMs={} durationMs={}",
                event.requestId(),
                event.core(),
                event.operation(),
                event.executorId(),
                event.status(),
                event.resultCode(),
                event.queueMs(),
                event.durationMs()
        );
    }

    private static String normalize(String value) {
        return Objects.requireNonNull(value, "value cannot be null.")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private static long elapsedMillis(Instant from, Instant to) {
        return Math.max(0L, Duration.between(from, to).toMillis());
    }

    private static final class Context {

        private final String requestId;
        private final String operation;
        private final Instant acceptedAt;

        private String executorId;
        private String username;
        private Instant assignedAt;
        private long queueMs;

        private Context(
                String requestId,
                String operation,
                Instant acceptedAt
        ) {
            this.requestId = requestId;
            this.operation = operation;
            this.acceptedAt = acceptedAt;
        }

        private synchronized void assign(
                String executorId,
                String username,
                long queueMs,
                Instant assignedAt
        ) {
            this.executorId = executorId;
            this.username = username;
            this.queueMs = queueMs;
            this.assignedAt = assignedAt;
        }

        private synchronized CoreOperationEvent toEvent(
                Instant completedAt,
                CoreOperationEvent.Status status,
                String resultCode,
                String errorType,
                Boolean retryable
        ) {
            return new CoreOperationEvent(
                    requestId,
                    CoreOperationEvent.Core.LEGACY,
                    "HERCULES",
                    "TK5R",
                    "KICKS_3270",
                    operation,
                    CoreOperationEvent.ExecutorType.TERMINAL_WORKER,
                    executorId,
                    username,
                    acceptedAt,
                    assignedAt,
                    completedAt,
                    queueMs,
                    elapsedMillis(acceptedAt, completedAt),
                    status,
                    resultCode,
                    errorType,
                    retryable
            );
        }
    }
}
