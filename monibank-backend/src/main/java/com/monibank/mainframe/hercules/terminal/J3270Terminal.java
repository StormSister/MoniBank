package com.monibank.mainframe.hercules.terminal;

import com.github.filipesimoes.j3270.Emulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class J3270Terminal extends Emulator {

    private static final Duration SCREEN_TIMEOUT =
            Duration.ofSeconds(20);

    private static final Duration COMMAND_TIMEOUT =
            Duration.ofSeconds(25);

    private Pattern secretPattern = Pattern.compile("(?!)");

    private boolean usable;

    public J3270Terminal(
            int controlPort,
            ExecutorService executor
    ) {
        super(controlPort, executor);
    }

    public void protectSecret(String secret) {
        Objects.requireNonNull(secret, "secret cannot be null.");

        if (secret.isEmpty()) {
            throw new IllegalArgumentException(
                    "secret cannot be empty."
            );
        }

        secretPattern = Pattern.compile(
                Pattern.quote(secret),
                Pattern.CASE_INSENSITIVE
        );
    }

    public boolean isUsable() {
        return usable;
    }

    public List<String> screen() {
        return command("Ascii()");
    }

    public void submit(String text) {
        command("Wait(15,Unlock)");
        command(stringCommand(text));
        command("Enter()");
    }

    public void typeKeys(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(
                    "Text entered with Key() cannot be empty."
            );
        }

        command("Wait(15,Unlock)");

        for (int i = 0; i < text.length(); i++) {
            char character =
                    Character.toUpperCase(text.charAt(i));

            if (!Character.isLetterOrDigit(character)) {
                throw new IllegalArgumentException(
                        "Unsupported Key() character at position " + i
                );
            }

            command("Key(" + character + ")");
        }
    }

    public void submitTransaction(String transaction) {
        if (!"MBGW".equals(transaction)
                && !"KSSF".equals(transaction)) {
            throw new IllegalArgumentException(
                    "Unknown KICKS transaction: " + transaction
            );
        }

        typeKeys(transaction);

        List<String> typed = screen();

        boolean transactionVisible = typed.stream()
                .map(String::trim)
                .anyMatch(transaction::equals);

        if (!transactionVisible) {
            throw new IllegalStateException(
                    "No confirmation for typing " + transaction
                            + ". Enter was not sent."
            );
        }

        command("Enter()");
    }

    public List<String> awaitScreen(
            Predicate<List<String>> expected
    ) throws InterruptedException {
        return awaitInternal(expected, false);
    }

    public List<String> await(
            Predicate<List<String>> expected
    ) throws InterruptedException {
        return awaitInternal(expected, true);
    }

    public void printScreen(List<String> screen) {
        for (int row = 0; row < screen.size(); row++) {
            String safe = secretPattern
                    .matcher(screen.get(row))
                    .replaceAll("[HIDDEN]");

            System.out.printf(
                    "%02d | %s%n",
                    row + 1,
                    safe
            );
        }
    }

    public List<String> command(String command) {
        Objects.requireNonNull(
                command,
                "command cannot be null."
        );

        return this.<List<String>>execute((writer, reader) -> {
            List<String> data = new ArrayList<>();
            String status = null;

            long deadline = System.nanoTime()
                    + COMMAND_TIMEOUT.toNanos();

            try {
                writer.write(command);
                writer.write("\n");
                writer.flush();

                for (int count = 0; count < 256; count++) {
                    String line = readLine(reader, deadline);

                    if (line.startsWith("data: ")) {
                        data.add(line.substring(6));
                        continue;
                    }

                    if ("ok".equals(line)) {
                        if (status == null) {
                            throw new IOException(
                                    "Missing emulator status."
                            );
                        }

                        usable = true;
                        return List.copyOf(data);
                    }

                    if ("error".equals(line)) {
                        usable = true;

                        throw rejectedCommand(
                                command,
                                status,
                                data
                        );
                    }

                    if (status != null) {
                        throw new IOException(
                                "Unexpected emulator response structure."
                        );
                    }

                    status = line;
                }

                throw new IOException(
                        "Emulator response line limit exceeded."
                );
            } catch (IOException exception) {
                usable = false;

                throw new UncheckedIOException(
                        "Communication with emulator failed.",
                        exception
                );
            }
        });
    }

    private List<String> awaitInternal(
            Predicate<List<String>> expected,
            boolean waitForUnlock
    ) throws InterruptedException {
        Objects.requireNonNull(
                expected,
                "expected predicate cannot be null."
        );

        long deadline = System.nanoTime()
                + SCREEN_TIMEOUT.toNanos();

        List<String> current = List.of();

        while (System.nanoTime() < deadline) {
            current = screen();

            if (expected.test(current)) {
                if (!waitForUnlock) {
                    return current;
                }

                command("Wait(15,Unlock)");
                current = screen();

                if (expected.test(current)) {
                    return current;
                }
            }

            Thread.sleep(200);
        }

        System.out.println(
                "=== SCREEN WHEN EXPECTATION FAILED ==="
        );
        printScreen(current);

        throw new IllegalStateException(
                "Expected screen did not appear."
        );
    }

    private CommandRejectedException rejectedCommand(
            String command,
            String status,
            List<String> data
    ) {
        int bracket = command.indexOf('(');

        String action = bracket > 0
                ? command.substring(0, bracket)
                : "unknown";

        boolean diagnostic =
                command.matches("Key\\([A-Z0-9]\\)")
                        || command.equals("Enter()")
                        || command.equals("Clear()");

        String details = diagnostic
                ? secretPattern
                .matcher(String.join(" | ", data))
                .replaceAll("[HIDDEN]")
                : "Arguments and response hidden.";

        return new CommandRejectedException(
                "Emulator rejected action: "
                        + action
                        + ". "
                        + details
                        + " Status: "
                        + status
        );
    }

    private static String stringCommand(String text) {
        Objects.requireNonNull(text, "text cannot be null.");

        StringBuilder command =
                new StringBuilder("String(\"");

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if (character < 32 || character > 126) {
                throw new IllegalArgumentException(
                        "Only printable ASCII characters are supported."
                );
            }

            command.append(
                    String.format(
                            "\\u%04x",
                            (int) character
                    )
            );
        }

        return command.append("\")").toString();
    }

    private static String readLine(
            BufferedReader reader,
            long deadline
    ) throws IOException {
        StringBuilder line = new StringBuilder();

        while (System.nanoTime() < deadline) {
            if (reader.ready()) {
                int character = reader.read();

                if (character == -1) {
                    throw new IOException(
                            "Emulator closed the connection."
                    );
                }

                if (character == '\n') {
                    return line.toString();
                }

                if (character != '\r') {
                    line.append((char) character);
                }

                if (line.length() > 16384) {
                    throw new IOException(
                            "Emulator response line is too long."
                    );
                }
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();

                    throw new IOException(
                            "Interrupted while reading emulator response.",
                            exception
                    );
                }
            }
        }

        throw new IOException(
                "Emulator response timeout after "
                        + COMMAND_TIMEOUT.toSeconds()
                        + " seconds."
        );
    }

    public static final class CommandRejectedException
            extends IllegalStateException {

        private static final long serialVersionUID = 1L;

        public CommandRejectedException(String message) {
            super(message);
        }
    }
}