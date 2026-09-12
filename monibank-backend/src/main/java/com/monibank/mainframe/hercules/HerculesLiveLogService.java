package com.monibank.mainframe.hercules;

import com.monibank.mainframe.port.MainframeLiveLogProcessFactory;
import com.monibank.mainframe.port.MainframeLiveLogPublisher;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class HerculesLiveLogService
        implements MainframeLiveLogPublisher {

    private static final Logger KICKS_RAW_LOG =
            LoggerFactory.getLogger("mainframe.kicks.raw");

    private static final int BUFFER_CAPACITY = 500;
    private static final int INITIAL_LINES = 100;
    private static final long RECONNECT_DELAY_MILLIS = 2_000L;
    private static final long EMITTER_TIMEOUT_MILLIS = 0L;

    private final MainframeLiveLogProcessFactory processFactory;
    private final Object monitor = new Object();
    private final Deque<LogLine> buffer = new ArrayDeque<>();
    private final List<SseEmitter> emitters = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicBoolean started = new AtomicBoolean();
    private final ExecutorService tailExecutor =
            Executors.newSingleThreadExecutor(
                    Thread.ofPlatform()
                            .daemon(true)
                            .name("hercules-log-tail")
                            .factory()
            );

    private volatile boolean running = true;
    private volatile Process tailProcess;

    public SseEmitter subscribe(Long lastEventId) {

        startTailerOnce();

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);

        emitter.onCompletion(() -> remove(emitter));
        emitter.onTimeout(() -> remove(emitter));
        emitter.onError(error -> remove(emitter));

        synchronized (monitor) {
            try {
                for (LogLine logLine : replayAfter(lastEventId)) {
                    send(emitter, logLine);
                }

                emitters.add(emitter);
            } catch (IOException | IllegalStateException e) {
                emitter.completeWithError(e);
            }
        }

        return emitter;
    }

    @Scheduled(fixedRate = 15_000L)
    public void keepConnectionsAlive() {

        synchronized (monitor) {
            Iterator<SseEmitter> iterator = emitters.iterator();

            while (iterator.hasNext()) {
                SseEmitter emitter = iterator.next();

                try {
                    emitter.send(
                            SseEmitter.event().comment("keepalive")
                    );
                } catch (IOException | IllegalStateException e) {
                    iterator.remove();
                    emitter.complete();
                }
            }
        }
    }

    private void startTailerOnce() {

        if (started.compareAndSet(false, true)) {
            tailExecutor.submit(this::runTailer);
        }
    }

    private void runTailer() {

        boolean firstConnection = true;

        while (running) {
            try {
                Process process = processFactory.start(
                        firstConnection ? INITIAL_LINES : 0
                );
                firstConnection = false;
                tailProcess = process;
                drainErrors(process);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                process.getInputStream(),
                                StandardCharsets.ISO_8859_1
                        )
                )) {
                    String line;

                    while (running && (line = reader.readLine()) != null) {
                        publish(LogSource.JES, line);
                    }
                }

                if (running) {
                    log.warn("Hercules live log process ended; reconnecting");
                }
            } catch (IOException e) {
                if (running) {
                    log.warn(
                            "Could not follow Hercules live log; reconnecting",
                            e
                    );
                }
            } finally {
                destroyTailProcess();
            }

            waitBeforeReconnect();
        }
    }

    private void drainErrors(Process process) {

        Thread.ofPlatform()
                .daemon(true)
                .name("hercules-log-tail-stderr")
                .start(() -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    process.getErrorStream(),
                                    StandardCharsets.UTF_8
                            )
                    )) {
                        reader.lines().forEach(
                                line -> log.warn(
                                        "Hercules live log process: {}",
                                        line
                                )
                        );
                    } catch (IOException e) {
                        if (running) {
                            log.debug(
                                    "Hercules live log error stream closed",
                                    e
                            );
                        }
                    }
                });
    }

    @Override
    public void publishKicks(String rawLine) {

        if (rawLine == null || rawLine.isBlank()) {
            return;
        }

        KICKS_RAW_LOG.info(rawLine);
        publish(LogSource.KICKS, rawLine);
    }

    private void publish(
            LogSource source,
            String rawLine
    ) {

        LogLine logLine = new LogLine(
                sequence.incrementAndGet(),
                source,
                rawLine
        );

        synchronized (monitor) {
            buffer.addLast(logLine);

            while (buffer.size() > BUFFER_CAPACITY) {
                buffer.removeFirst();
            }

            Iterator<SseEmitter> iterator = emitters.iterator();

            while (iterator.hasNext()) {
                SseEmitter emitter = iterator.next();

                try {
                    send(emitter, logLine);
                } catch (IOException | IllegalStateException e) {
                    iterator.remove();
                    emitter.complete();
                }
            }
        }
    }

    private List<LogLine> replayAfter(Long lastEventId) {

        if (lastEventId == null) {
            return buffer.stream()
                    .skip(Math.max(0, buffer.size() - INITIAL_LINES))
                    .toList();
        }

        return buffer.stream()
                .filter(line -> line.id() > lastEventId)
                .toList();
    }

    private void send(
            SseEmitter emitter,
            LogLine logLine
    ) throws IOException {

        emitter.send(
                SseEmitter.event()
                        .id(String.valueOf(logLine.id()))
                        .name(logLine.source().eventName())
                        .data(logLine.rawLine())
        );
    }

    private void remove(SseEmitter emitter) {

        synchronized (monitor) {
            emitters.remove(emitter);
        }
    }

    private void waitBeforeReconnect() {

        if (!running) {
            return;
        }

        try {
            Thread.sleep(RECONNECT_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void destroyTailProcess() {

        Process process = tailProcess;
        tailProcess = null;

        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

    @PreDestroy
    public void stop() {

        running = false;
        destroyTailProcess();
        tailExecutor.shutdownNow();

        synchronized (monitor) {
            emitters.forEach(SseEmitter::complete);
            emitters.clear();
        }
    }

    private record LogLine(
            long id,
            LogSource source,
            String rawLine
    ) {
    }

    private enum LogSource {
        JES("jes"),
        KICKS("kicks");

        private final String eventName;

        LogSource(String eventName) {
            this.eventName = eventName;
        }

        private String eventName() {
            return eventName;
        }
    }
}
