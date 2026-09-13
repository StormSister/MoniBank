package com.monibank.mainframe.operations;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@Component
public final class FileOperationJournal
        implements OperationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(
            FileOperationJournal.class
    );
    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ISO_LOCAL_DATE;

    private final Path directory;
    private final int retentionDays;
    private LocalDate lastCleanupDate;

    @Autowired
    public FileOperationJournal(
            @Value("${monibank.operations.journal-directory:logs/operations}")
            String journalDirectory,
            @Value("${monibank.operations.retention-days:30}")
            int retentionDays
    ) {
        if (journalDirectory == null || journalDirectory.isBlank()) {
            throw new IllegalArgumentException(
                    "Operation journal directory cannot be blank."
            );
        }
        this.directory = Path.of(journalDirectory).normalize();
        if (retentionDays < 1 || retentionDays > 3_650) {
            throw new IllegalArgumentException(
                    "Operation retention must be between 1 and 3650 days."
            );
        }
        this.retentionDays = retentionDays;
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(directory);
            log.info(
                    "Operation journal directory: {} (retention: {} days)",
                    directory.toAbsolutePath(),
                    retentionDays
            );
            cleanupExpiredFiles(LocalDate.now(ZoneOffset.UTC));
        } catch (IOException exception) {
            log.error(
                    "Could not initialize operation journal directory {}",
                    directory.toAbsolutePath(),
                    exception
            );
        }
    }

    @Override
    public synchronized void publish(CoreOperationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null.");
        }

        try {
            Files.createDirectories(directory);
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            if (!today.equals(lastCleanupDate)) {
                cleanupExpiredFiles(today);
            }
            Files.writeString(
                    fileFor(event),
                    OperationEventJsonCodec.encode(event)
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            /*
             * Observability must not turn a successful banking operation
             * into an application failure. The event remains in normal logs.
             */
            log.error(
                    "Could not append operation event {} to {}",
                    event.requestId(),
                    directory.toAbsolutePath(),
                    exception
            );
        }
    }

    private Path fileFor(CoreOperationEvent event) {
        String date = FILE_DATE.format(
                event.completedAt().atZone(ZoneOffset.UTC)
        );
        return directory.resolve("operations-" + date + ".jsonl");
    }

    private void cleanupExpiredFiles(LocalDate today) {
        LocalDate oldestRetainedDate = today.minusDays(retentionDays - 1L);

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(this::isExpiredOperationFile)
                    .filter(path -> fileDate(path).isBefore(oldestRetainedDate))
                    .forEach(this::deleteExpiredFile);
            lastCleanupDate = today;
        } catch (IOException exception) {
            log.warn(
                    "Could not clean expired operation journal files in {}",
                    directory.toAbsolutePath(),
                    exception
            );
        }
    }

    private boolean isExpiredOperationFile(Path path) {
        String name = path.getFileName().toString();
        if (!name.startsWith("operations-") || !name.endsWith(".jsonl")) {
            return false;
        }
        try {
            fileDate(path);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private LocalDate fileDate(Path path) {
        String name = path.getFileName().toString();
        String date = name.substring(
                "operations-".length(),
                name.length() - ".jsonl".length()
        );
        return LocalDate.parse(date, FILE_DATE);
    }

    private void deleteExpiredFile(Path path) {
        try {
            Files.deleteIfExists(path);
            log.info("Deleted expired operation journal {}", path);
        } catch (IOException exception) {
            log.warn("Could not delete expired operation journal {}", path);
        }
    }
}
