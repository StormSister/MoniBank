package com.monibank.mainframe;

import com.monibank.mainframe.hercules.VsamAccessCoordinator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VsamAccessCoordinatorTest {

    @Test
    void allowsReadOnlyOperationsToRunConcurrently() throws Exception {
        VsamAccessCoordinator coordinator = new VsamAccessCoordinator();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch bothEntered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        try {
            Future<?> first = executor.submit(() -> coordinator.execute(
                    "R0000001",
                    "LISTCUST",
                    () -> waitInside(bothEntered, release)
            ));
            Future<?> second = executor.submit(() -> coordinator.execute(
                    "R0000002",
                    "LISTACCT",
                    () -> waitInside(bothEntered, release)
            ));

            assertTrue(
                    bothEntered.await(1, TimeUnit.SECONDS),
                    "Both read-only operations should enter together."
            );
            release.countDown();
            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void serializesMutatingAndUnknownOperations() throws Exception {
        VsamAccessCoordinator coordinator = new VsamAccessCoordinator();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);

        try {
            Future<?> first = executor.submit(() -> coordinator.execute(
                    "R0000001",
                    "ADDCUST",
                    () -> waitInside(firstEntered, releaseFirst)
            ));
            assertTrue(firstEntered.await(1, TimeUnit.SECONDS));

            Future<?> second = executor.submit(() -> coordinator.execute(
                    "R0000002",
                    "FUTUREOP",
                    () -> {
                        secondEntered.countDown();
                        return null;
                    }
            ));

            awaitWaitingRequest(coordinator);
            assertFalse(
                    secondEntered.await(100, TimeUnit.MILLISECONDS),
                    "A second write must wait for the first write."
            );

            releaseFirst.countDown();
            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);
            assertEquals(0, coordinator.waitingRequestCount());
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void blocksReadsWhileAWriteIsRunning() throws Exception {
        VsamAccessCoordinator coordinator = new VsamAccessCoordinator();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch writeEntered = new CountDownLatch(1);
        CountDownLatch releaseWrite = new CountDownLatch(1);
        CountDownLatch readEntered = new CountDownLatch(1);

        try {
            Future<?> write = executor.submit(() -> coordinator.execute(
                    "R0000001",
                    "ADDACCT",
                    () -> waitInside(writeEntered, releaseWrite)
            ));
            assertTrue(writeEntered.await(1, TimeUnit.SECONDS));

            Future<?> read = executor.submit(() -> coordinator.execute(
                    "R0000002",
                    "LISTACCT",
                    () -> {
                        readEntered.countDown();
                        return null;
                    }
            ));

            awaitWaitingRequest(coordinator);
            assertFalse(readEntered.await(100, TimeUnit.MILLISECONDS));

            releaseWrite.countDown();
            write.get(1, TimeUnit.SECONDS);
            read.get(1, TimeUnit.SECONDS);
        } finally {
            releaseWrite.countDown();
            executor.shutdownNow();
        }
    }

    private static Void waitInside(
            CountDownLatch entered,
            CountDownLatch release
    ) {
        entered.countDown();
        try {
            if (!release.await(1, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for release.");
            }
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static void awaitWaitingRequest(
            VsamAccessCoordinator coordinator
    ) {
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> {
                    while (coordinator.waitingRequestCount() == 0) {
                        Thread.onSpinWait();
                    }
                }
        );
    }
}
