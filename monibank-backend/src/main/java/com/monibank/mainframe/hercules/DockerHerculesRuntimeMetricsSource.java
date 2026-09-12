package com.monibank.mainframe.hercules;

import com.monibank.mainframe.config.MainframeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DockerHerculesRuntimeMetricsSource {

    private static final String FIELD_SEPARATOR = "__MB__";
    private static final long COMMAND_TIMEOUT_SECONDS = 10L;

    private final MainframeProperties properties;

    public HerculesRuntimeMetrics read() throws IOException {

        String container = properties.liveLogContainer();

        if (container == null || container.isBlank()) {
            throw new IOException(
                    "MAINFRAME_LIVE_LOG_CONTAINER is not configured"
            );
        }

        String inspect = runDocker(
                "inspect",
                "--format={{.State.Status}}"
                        + FIELD_SEPARATOR
                        + "{{.State.StartedAt}}",
                container
        );

        String stats = runDocker(
                "stats",
                "--no-stream",
                "--format={{.CPUPerc}}"
                        + FIELD_SEPARATOR
                        + "{{.MemUsage}}",
                container
        );

        String[] inspectFields = split(inspect, 2, "docker inspect");
        String[] statsFields = split(stats, 2, "docker stats");
        String[] memoryFields = statsFields[1].split("\\s*/\\s*", 2);

        if (memoryFields.length != 2) {
            throw new IOException(
                    "Unexpected Docker memory value: " + statsFields[1]
            );
        }

        Instant startedAt;

        try {
            startedAt = Instant.parse(inspectFields[1].trim());
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Unexpected Docker start time: " + inspectFields[1],
                    exception
            );
        }

        double usedBytes = parseBytes(memoryFields[0]);
        double limitBytes = parseBytes(memoryFields[1]);

        return new HerculesRuntimeMetrics(
                inspectFields[0].trim().toUpperCase(Locale.ROOT),
                parsePercent(statsFields[0]),
                memoryFields[0].trim(),
                memoryFields[1].trim(),
                limitBytes <= 0.0
                        ? 0.0
                        : usedBytes * 100.0 / limitBytes,
                Math.max(
                        0L,
                        Duration.between(startedAt, Instant.now()).toSeconds()
                ),
                Instant.now()
        );
    }

    private String runDocker(String... dockerArguments)
            throws IOException {

        List<String> command = new ArrayList<>();

        if (properties.logHost() != null
                && !properties.logHost().isBlank()) {
            command.addAll(List.of(
                    "ssh",
                    "-o", "BatchMode=yes",
                    "-o", "ConnectTimeout=10",
                    "-o", "LogLevel=ERROR",
                    properties.logUser() + "@" + properties.logHost()
            ));
        }

        command.add("docker");
        command.addAll(Arrays.asList(dockerArguments));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        try {
            if (!process.waitFor(
                    COMMAND_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            )) {
                process.destroyForcibly();
                throw new IOException("Docker metrics command timed out");
            }

            String output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            ).strip();

            if (process.exitValue() != 0) {
                throw new IOException(
                        "Docker metrics command failed: " + output
                );
            }

            return output.lines()
                    .filter(line -> !line.isBlank())
                    .findFirst()
                    .orElseThrow(() ->
                            new IOException(
                                    "Docker metrics command returned no data"
                            )
                    );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Docker metrics command was interrupted",
                    exception
            );
        }
    }

    private String[] split(
            String value,
            int expectedFields,
            String commandName
    ) throws IOException {

        String[] fields = value.split(FIELD_SEPARATOR, -1);

        if (fields.length != expectedFields) {
            throw new IOException(
                    "Unexpected " + commandName + " output: " + value
            );
        }

        return fields;
    }

    private double parsePercent(String value) throws IOException {

        try {
            return Double.parseDouble(
                    value.replace("%", "")
                            .replace(',', '.')
                            .trim()
            );
        } catch (NumberFormatException exception) {
            throw new IOException(
                    "Unexpected Docker percentage: " + value,
                    exception
            );
        }
    }

    private double parseBytes(String value) throws IOException {

        String normalized = value.trim().replace(',', '.');
        int unitStart = 0;

        while (unitStart < normalized.length()
                && (Character.isDigit(normalized.charAt(unitStart))
                || normalized.charAt(unitStart) == '.')) {
            unitStart++;
        }

        if (unitStart == 0) {
            throw new IOException(
                    "Unexpected Docker memory value: " + value
            );
        }

        double amount;

        try {
            amount = Double.parseDouble(
                    normalized.substring(0, unitStart)
            );
        } catch (NumberFormatException exception) {
            throw new IOException(
                    "Unexpected Docker memory value: " + value,
                    exception
            );
        }

        String unit = normalized.substring(unitStart)
                .trim()
                .toUpperCase(Locale.ROOT);

        double multiplier = switch (unit) {
            case "B" -> 1.0;
            case "KB", "KIB" -> 1024.0;
            case "MB", "MIB" -> 1024.0 * 1024.0;
            case "GB", "GIB" -> 1024.0 * 1024.0 * 1024.0;
            default -> throw new IOException(
                    "Unsupported Docker memory unit: " + unit
            );
        };

        return amount * multiplier;
    }
}
