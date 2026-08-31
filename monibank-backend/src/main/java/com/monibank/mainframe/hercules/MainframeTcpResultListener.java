package com.monibank.mainframe.hercules;

import com.monibank.mainframe.config.MainframeProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainframeTcpResultListener {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final long RECONNECT_DELAY_MS = 2_000;

    private static final String RESULT_PREFIX = "MBR;";
    private static final String FRAME_PREFIX = "MBP;";
    private static final String RESULT_SEPARATOR = ";";

    private static final int FRAME_PAYLOAD_LENGTH = 80;
    private static final int LOGICAL_RECORD_LENGTH = 160;

    private final MainframeProperties properties;

    private final Map<String, PendingResult> pendingRequests =
            new ConcurrentHashMap<>();

    private volatile boolean running;
    private volatile Socket socket;

    private Thread listenerThread;

    @PostConstruct
    public void start() {

        running = true;

        listenerThread = new Thread(
                this::listenLoop,
                "mainframe-result-listener"
        );

        listenerThread.setDaemon(true);
        listenerThread.start();

        log.info(
                "Mainframe TCP result listener started for {}:{}",
                properties.resultHost(),
                properties.resultPort()
        );
    }

    public void register(String requestId) {

        PendingResult pending =
                new PendingResult();

        PendingResult previous =
                pendingRequests.putIfAbsent(
                        requestId,
                        pending
                );

        if (previous != null) {
            throw new IllegalStateException(
                    "Mainframe request already registered: "
                            + requestId
            );
        }

        log.debug(
                "Registered mainframe request {}. Pending={}",
                requestId,
                pendingRequests.size()
        );
    }

    public List<String> await(
            String requestId,
            Duration timeout
    ) throws TimeoutException {

        PendingResult pending =
                pendingRequests.get(requestId);

        if (pending == null) {
            throw new IllegalStateException(
                    "Mainframe request is not registered: "
                            + requestId
            );
        }

        try {

            return pending.future()
                    .get(
                            timeout.toMillis(),
                            TimeUnit.MILLISECONDS
                    );

        } catch (TimeoutException e) {

            throw e;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting for mainframe result: "
                            + requestId,
                    e
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed while waiting for mainframe result: "
                            + requestId,
                    e
            );
        }
    }

    public void unregister(String requestId) {

        pendingRequests.remove(requestId);

        log.debug(
                "Unregistered mainframe request {}. Pending={}",
                requestId,
                pendingRequests.size()
        );
    }

    private void listenLoop() {

        while (running) {

            try {

                connectAndRead();

            } catch (Exception e) {

                if (!running) {
                    return;
                }

                log.warn(
                        "Mainframe result TCP connection lost: {}",
                        e.getMessage()
                );

                sleepBeforeReconnect();
            }
        }
    }

    private void connectAndRead()
            throws IOException {

        Socket newSocket =
                new Socket();

        newSocket.setKeepAlive(true);

        newSocket.connect(
                new InetSocketAddress(
                        properties.resultHost(),
                        properties.resultPort()
                ),
                CONNECT_TIMEOUT_MS
        );

        socket = newSocket;

        log.info(
                "Connected to mainframe result printer {}:{}",
                properties.resultHost(),
                properties.resultPort()
        );

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        newSocket.getInputStream(),
                                        StandardCharsets.US_ASCII
                                )
                        )
        ) {

            String line;

            while (running
                    && (line = reader.readLine()) != null) {

                handleLine(line);
            }

        } finally {

            closeSocket();
        }

        if (running) {
            throw new IOException(
                    "Mainframe result printer closed TCP connection"
            );
        }
    }

    private void handleLine(String rawLine) {

        String line =
                normalize(rawLine);

        if (line.isBlank()) {
            return;
        }

        if (line.startsWith(FRAME_PREFIX)) {

            reassembleFrame(line)
                    .ifPresent(this::handleResultRecord);

            return;
        }

        if (line.startsWith(RESULT_PREFIX)) {
            handleResultRecord(line);
        }
    }

    private Optional<String> reassembleFrame(
            String frame
    ) {

        /*
         * MBP;<requestId>;<part>;<80 payload characters>
         *
         * Split is limited to four fields because the payload is an
         * arbitrary half of an MBR record and may contain semicolons.
         */
        String[] parts =
                frame.split(
                        RESULT_SEPARATOR,
                        4
                );

        if (parts.length != 4) {
            log.warn(
                    "Ignoring invalid mainframe result frame: [{}]",
                    frame
            );
            return Optional.empty();
        }

        String requestId =
                parts[1].trim();

        String part =
                parts[2].trim();

        if (requestId.isBlank()) {
            log.warn(
                    "Mainframe result frame has no requestId: [{}]",
                    frame
            );
            return Optional.empty();
        }

        if (!"1".equals(part)
                && !"2".equals(part)) {
            log.warn(
                    "Invalid frame part {} for request {}",
                    part,
                    requestId
            );
            return Optional.empty();
        }

        String payload =
                padFramePayload(
                        requestId,
                        part,
                        parts[3]
                );

        if (payload == null) {
            return Optional.empty();
        }

        PendingResult pending =
                pendingRequests.get(requestId);

        if (pending == null) {
            log.warn(
                    "Received frame for unknown or expired request {}",
                    requestId
            );
            return Optional.empty();
        }

        Optional<String> logicalRecord =
                pending.acceptFrame(
                        requestId,
                        part,
                        payload
                );

        logicalRecord.ifPresent(record ->
                log.info(
                        "MAINFRAME FRAME [{}] reassembled into {} bytes",
                        requestId,
                        record.length()
                )
        );

        return logicalRecord;
    }

    private String padFramePayload(
            String requestId,
            String part,
            String payload
    ) {

        /*
         * Some printer paths remove trailing spaces. They are restored
         * here because each MBRESULT frame always carries exactly 80
         * logical payload characters.
         */
        if (payload.length() > FRAME_PAYLOAD_LENGTH) {
            log.warn(
                    "Frame {} for request {} has {} payload bytes; max is {}",
                    part,
                    requestId,
                    payload.length(),
                    FRAME_PAYLOAD_LENGTH
            );
            return null;
        }

        return payload
                + " ".repeat(
                FRAME_PAYLOAD_LENGTH
                        - payload.length()
        );
    }

    private void handleResultRecord(String line) {

        if (line.length() != LOGICAL_RECORD_LENGTH) {
            log.debug(
                    "Logical MBR record has {} characters instead of {}",
                    line.length(),
                    LOGICAL_RECORD_LENGTH
            );
        }

        log.info(
                "MAINFRAME RESULT << [{}]",
                line
        );

        String[] parts =
                line.split(
                        RESULT_SEPARATOR,
                        -1
                );

        if (parts.length < 4) {
            log.warn(
                    "Ignoring invalid mainframe result record: [{}]",
                    line
            );
            return;
        }

        String type =
                parts[1].trim();

        String requestId =
                parts[3].trim();

        if (requestId.isBlank()) {
            log.warn(
                    "Mainframe result has no requestId: [{}]",
                    line
            );
            return;
        }

        PendingResult pending =
                pendingRequests.get(requestId);

        if (pending == null) {
            log.warn(
                    "Received result for unknown or expired request {}: [{}]",
                    requestId,
                    line
            );
            return;
        }

        pending.add(line);

        switch (type) {

            case "S", "E" ->
                    pending.complete();

            case "D" -> {
                // Wait for more logical records for this request.
            }

            default ->
                    log.warn(
                            "Unknown mainframe result type {} for request {}",
                            type,
                            requestId
                    );
        }
    }

    private String normalize(String rawLine) {

        if (rawLine == null) {
            return "";
        }

        /*
         * stripLeading removes the optional ASA carriage-control blank.
         * Trailing spaces are deliberately preserved.
         */
        return rawLine
                .replace("\f", "")
                .replace("\r", "")
                .stripLeading();
    }

    private void sleepBeforeReconnect() {

        try {

            Thread.sleep(
                    RECONNECT_DELAY_MS
            );

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();
        }
    }

    private void closeSocket() {

        Socket current =
                socket;

        socket = null;

        if (current == null) {
            return;
        }

        try {

            current.close();

        } catch (IOException e) {

            log.debug(
                    "Error closing mainframe result socket",
                    e
            );
        }
    }

    @PreDestroy
    public void stop() {

        running = false;

        closeSocket();

        if (listenerThread != null) {
            listenerThread.interrupt();
        }

        pendingRequests.forEach(
                (requestId, pending) ->
                        pending.future()
                                .completeExceptionally(
                                        new IllegalStateException(
                                                "Application shutting down"
                                        )
                                )
        );

        pendingRequests.clear();
    }

    private static final class PendingResult {

        private final List<String> records =
                new ArrayList<>();

        private final CompletableFuture<List<String>> future =
                new CompletableFuture<>();

        private String firstFramePayload;

        public synchronized Optional<String> acceptFrame(
                String requestId,
                String part,
                String payload
        ) {

            if (future.isDone()) {
                return Optional.empty();
            }

            if ("1".equals(part)) {

                if (firstFramePayload != null) {
                    log.warn(
                            "Replacing unfinished frame 1 for request {}",
                            requestId
                    );
                }

                firstFramePayload = payload;
                return Optional.empty();
            }

            if (firstFramePayload == null) {
                log.warn(
                        "Frame 2 arrived without frame 1 for request {}",
                        requestId
                );
                return Optional.empty();
            }

            String record =
                    firstFramePayload
                            + payload;

            firstFramePayload = null;

            if (record.length() != LOGICAL_RECORD_LENGTH) {
                log.warn(
                        "Reassembled record for request {} has {} bytes",
                        requestId,
                        record.length()
                );
                return Optional.empty();
            }

            if (!record.startsWith(RESULT_PREFIX)) {
                log.warn(
                        "Reassembled record for request {} is not MBR",
                        requestId
                );
                return Optional.empty();
            }

            String[] fields =
                    record.split(
                            RESULT_SEPARATOR,
                            5
                    );

            if (fields.length < 4
                    || !requestId.equals(fields[3].trim())) {
                log.warn(
                        "Frame requestId {} does not match logical record",
                        requestId
                );
                return Optional.empty();
            }

            return Optional.of(record);
        }

        public synchronized void add(
                String record
        ) {

            if (future.isDone()) {
                return;
            }

            records.add(record);
        }

        public synchronized void complete() {

            if (future.isDone()) {
                return;
            }

            firstFramePayload = null;

            future.complete(
                    List.copyOf(records)
            );
        }

        public CompletableFuture<List<String>> future() {
            return future;
        }
    }
}
