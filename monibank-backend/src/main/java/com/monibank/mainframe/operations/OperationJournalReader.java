package com.monibank.mainframe.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public final class OperationJournalReader {

    private static final Logger log = LoggerFactory.getLogger(
            OperationJournalReader.class
    );

    private final Path directory;

    public OperationJournalReader(
            @Value("${monibank.operations.journal-directory:logs/operations}")
            String journalDirectory
    ) {
        this.directory = Path.of(journalDirectory).normalize();
    }

    public List<CoreOperationEvent> findSince(Instant from) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }

        List<CoreOperationEvent> events = new ArrayList<>();
        LocalDate firstDate = from.atZone(ZoneOffset.UTC).toLocalDate();
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> operationFileDate(path)
                            .map(date -> !date.isBefore(firstDate))
                            .orElse(false))
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> readFile(path, from, events));
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read operation journal directory.",
                    exception
            );
        }

        return events.stream()
                .sorted(Comparator.comparing(
                        CoreOperationEvent::completedAt
                ).reversed())
                .toList();
    }

    private void readFile(
            Path path,
            Instant from,
            List<CoreOperationEvent> events
    ) {
        try {
            for (String line : Files.readAllLines(
                    path,
                    StandardCharsets.UTF_8
            )) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    CoreOperationEvent event =
                            OperationEventJsonCodec.decode(line);
                    if (!event.completedAt().isBefore(from)) {
                        events.add(event);
                    }
                } catch (RuntimeException exception) {
                    log.warn(
                            "Skipping malformed operation record in {}",
                            path.getFileName(),
                            exception
                    );
                }
            }
        } catch (IOException exception) {
            log.warn(
                    "Could not read operation journal {}",
                    path.getFileName(),
                    exception
            );
        }
    }

    private Optional<LocalDate> operationFileDate(Path path) {
        String name = path.getFileName().toString();
        if (!name.startsWith("operations-")
                || !name.endsWith(".jsonl")) {
            return Optional.empty();
        }

        String date = name.substring(
                "operations-".length(),
                name.length() - ".jsonl".length()
        );
        try {
            return Optional.of(LocalDate.parse(date));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }
}
