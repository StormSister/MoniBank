package com.monibank.mainframe.hercules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Coordinates access to VSAM datasets shared by independent KICKS regions.
 *
 * <p>Known read-only operations may run concurrently. Every mutating or
 * unknown operation is exclusive. Treating unknown operations as writes is
 * deliberate: adding a new operation cannot silently bypass serialization.</p>
 */
@Component
public final class VsamAccessCoordinator {

    private static final Logger log = LoggerFactory.getLogger(
            VsamAccessCoordinator.class
    );

    private static final Set<String> READ_ONLY_OPERATIONS = Set.of(
            "GETCUST",
            "LISTCUST",
            "LISTACCT",
            "LISTCARD",
            "LISTTXN",
            "GETSTAT"
    );

    private final ReentrantReadWriteLock accessLock =
            new ReentrantReadWriteLock(true);
    private final AtomicInteger waitingRequests = new AtomicInteger();

    public <T> T execute(
            String requestId,
            String operation,
            Supplier<T> action
    ) {
        String normalizedOperation = normalize(operation);
        boolean readOnly = READ_ONLY_OPERATIONS.contains(
                normalizedOperation
        );
        Lock lock = readOnly
                ? accessLock.readLock()
                : accessLock.writeLock();
        String accessMode = readOnly ? "READ" : "WRITE";
        long waitStartedAt = System.nanoTime();

        waitingRequests.incrementAndGet();
        try {
            lock.lock();
        } finally {
            waitingRequests.decrementAndGet();
        }

        long waitMs = (System.nanoTime() - waitStartedAt) / 1_000_000L;
        try {
            log.info(
                    "VSAM access granted requestId={} operation={} mode={} waitMs={}",
                    requestId,
                    normalizedOperation,
                    accessMode,
                    waitMs
            );
            return action.get();
        } finally {
            lock.unlock();
            log.info(
                    "VSAM access released requestId={} operation={} mode={}",
                    requestId,
                    normalizedOperation,
                    accessMode
            );
        }
    }

    public int waitingRequestCount() {
        return waitingRequests.get();
    }

    private static String normalize(String operation) {
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException(
                    "VSAM operation cannot be blank."
            );
        }
        return operation.toUpperCase(Locale.ROOT);
    }
}
