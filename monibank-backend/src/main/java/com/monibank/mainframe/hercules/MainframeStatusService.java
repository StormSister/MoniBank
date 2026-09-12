package com.monibank.mainframe.hercules;

import com.monibank.mainframe.api.MainframeStatusResponse;
import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.hercules.terminal.KicksTerminalSessionManager;
import com.monibank.mainframe.hercules.terminal.TerminalSessionSnapshot;
import com.monibank.mainframe.hercules.terminal.TerminalSessionState;
import com.monibank.mainframe.port.MainframeGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MainframeStatusService {

    private static final Duration READER_CHECK_INTERVAL =
            Duration.ofMinutes(1);

    private final MainframeGateway mainframeGateway;
    private final MainframeProperties properties;
    private final MainframeTcpResultListener resultListener;
    private final DockerHerculesRuntimeMetricsSource metricsSource;
    private final ObjectProvider<KicksTerminalSessionManager>
            terminalManagerProvider;

    private volatile HerculesRuntimeMetrics lastRuntimeMetrics;
    private volatile boolean readerAvailable;
    private volatile Instant lastReaderCheck = Instant.EPOCH;
    private volatile MainframeStatusResponse current;

    @Scheduled(
            fixedDelayString =
                    "${monibank.mainframe.metrics-refresh-ms:15000}",
            initialDelayString =
                    "${monibank.mainframe.metrics-initial-delay-ms:1000}"
    )
    public void refresh() {

        refreshReaderAvailabilityIfDue();
        boolean stale = false;
        HerculesRuntimeMetrics runtime = lastRuntimeMetrics;

        try {
            runtime = metricsSource.read();
            lastRuntimeMetrics = runtime;
        } catch (IOException exception) {
            stale = true;
            log.warn(
                    "Could not refresh Hercules runtime metrics: {}",
                    exception.getMessage()
            );
        }

        KicksTerminalSessionManager terminalManager =
                terminalManagerProvider.getIfAvailable();

        String terminalState = terminalManager == null
                ? "DISABLED"
                : terminalManager.state().name();

        List<TerminalSessionSnapshot> terminalSnapshots =
                terminalManager == null
                        ? List.of()
                        : terminalManager.terminalSnapshots();
        int configuredTerminals = terminalSnapshots.size();
        int readyTerminals = countState(
                terminalSnapshots,
                TerminalSessionState.READY
        );
        int busyTerminals = countState(
                terminalSnapshots,
                TerminalSessionState.BUSY
        );
        int recoveringTerminals = configuredTerminals
                - readyTerminals
                - busyTerminals;

        int queuedRequests = terminalManager == null
                ? 0
                : terminalManager.queuedRequestCount();

        boolean resultPrinterConnected = resultListener.isConnected();
        String overallStatus = overallStatus(
                runtime,
                readerAvailable,
                resultPrinterConnected,
                terminalManager != null,
                configuredTerminals,
                readyTerminals + busyTerminals
        );

        current = new MainframeStatusResponse(
                properties.provider(),
                "MVS 3.8j",
                "TK5R",
                overallStatus,
                new MainframeStatusResponse.ConnectionStatus(
                        readerAvailable ? "CONNECTED" : "DISCONNECTED",
                        resultPrinterConnected
                                ? "CONNECTED"
                                : "DISCONNECTED",
                        terminalState,
                        configuredTerminals,
                        readyTerminals,
                        busyTerminals,
                        recoveringTerminals,
                        queuedRequests,
                        terminalSnapshots.stream()
                                .map(MainframeStatusService::toStatus)
                                .toList()
                ),
                runtime,
                stale,
                Instant.now()
        );
    }

    private String overallStatus(
            HerculesRuntimeMetrics runtime,
            boolean readerConnected,
            boolean resultPrinterConnected,
            boolean terminalEnabled,
            int configuredTerminals,
            int operationalTerminals
    ) {
        if (runtime != null
                && !"RUNNING".equals(runtime.containerState())) {
            return "OFFLINE";
        }

        boolean terminalOperational = !terminalEnabled
                || (configuredTerminals > 0
                && operationalTerminals == configuredTerminals);

        if (runtime != null
                && readerConnected
                && resultPrinterConnected
                && terminalOperational) {
            return "ONLINE";
        }

        if (runtime != null
                || readerConnected
                || resultPrinterConnected
                || (terminalEnabled && operationalTerminals > 0)) {
            return "DEGRADED";
        }

        return "OFFLINE";
    }

    public MainframeStatusResponse current() {
        MainframeStatusResponse snapshot = current;
        return snapshot == null ? initialStatus() : snapshot;
    }

    private void refreshReaderAvailabilityIfDue() {
        Instant now = Instant.now();

        if (Duration.between(lastReaderCheck, now)
                .compareTo(READER_CHECK_INTERVAL) < 0) {
            return;
        }

        readerAvailable = mainframeGateway.isAvailable();
        lastReaderCheck = now;
    }

    private MainframeStatusResponse initialStatus() {
        return new MainframeStatusResponse(
                properties.provider(),
                "MVS 3.8j",
                "TK5R",
                "CHECKING",
                new MainframeStatusResponse.ConnectionStatus(
                        "CHECKING",
                        "CHECKING",
                        "CHECKING",
                        0,
                        0,
                        0,
                        0,
                        0,
                        List.of()
                ),
                null,
                true,
                Instant.now()
        );
    }

    private static int countState(
            List<TerminalSessionSnapshot> snapshots,
            TerminalSessionState state
    ) {
        return (int) snapshots.stream()
                .filter(snapshot -> snapshot.state() == state)
                .count();
    }

    private static MainframeStatusResponse.TerminalStatus toStatus(
            TerminalSessionSnapshot snapshot
    ) {
        return new MainframeStatusResponse.TerminalStatus(
                snapshot.id(),
                snapshot.username(),
                snapshot.state().name(),
                snapshot.currentRequestId(),
                snapshot.recoveryCount(),
                snapshot.readySince(),
                snapshot.lastFailureAt(),
                snapshot.lastError()
        );
    }
}
