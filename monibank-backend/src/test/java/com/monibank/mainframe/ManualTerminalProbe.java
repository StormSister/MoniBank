package com.monibank.mainframe;

import com.github.filipesimoes.j3270.Emulator;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.regex.Pattern;

class ManualTerminalProbe {

    private static final String PASSWORD_PROMPT =
            "ENTER CURRENT PASSWORD FOR HERC01-";

    private static final String START_KICKS =
            "EXEC 'HERC01.CMDPROC(MBKICKS)'";

    @Test
    void readInitialScreen() throws Exception {
        String password = System.getenv("MB_TSO_PASSWORD");

        if (password == null || password.isEmpty()) {
            throw new IllegalStateException(
                    "Brak MB_TSO_PASSWORD. Ustaw haslo w PowerShell."
            );
        }

        String passwordCommand = stringCommand(password);
        Pattern secret = Pattern.compile(
                Pattern.quote(password), Pattern.CASE_INSENSITIVE
        );

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (ProbeEmulator terminal = new ProbeEmulator(13270, executor)) {
            terminal.setVisible(false);
            terminal.setModel("3279-2");
            terminal.secret = secret;

            boolean passwordAttempted = false;
            boolean kicksLaunched = false;
            boolean mapReached = false;
            boolean loggedOff = false;

            try {
                System.out.println("Starting emulator...");
                terminal.start();

                System.out.println("Connectiong throught SSH...");
                terminal.command("Connect(127.0.0.1:13271)");

                List<String> screen = terminal.awaitScreen(
                        s -> isLoginEntry(s) || isHerculesWelcome(s),
                        secret
                );

// Dodatkowy Enter tylko na powitaniu Herculesa.
                if (!isLoginEntry(screen) && isHerculesWelcome(screen)) {
                    System.out.println("Hercules welcoming screen: Enter.");
                    terminal.command("Enter()");

                    screen = terminal.awaitScreen(
                            ManualTerminalProbe::isLoginEntry,
                            secret
                    );
                }

// Nie wymagamy sformatowanego pola na ekranie INPUT NOT RECOGNIZED.
                terminal.command("Wait(15,Unlock)");

                System.out.println("Typing login HERC01...");

                if (isLogon(screen)) {
                    terminal.command("Wait(15,InputField)");
                    terminal.submit("HERC01");
                } else {
                    // Wariant wejscia bez standardowego napisu Logon.
                    terminal.submit("LOGON HERC01");
                }


                screen = terminal.await(
                        s -> contains(s, PASSWORD_PROMPT)
                                || contains(s, "LOGON REJECTED"),
                        secret
                );

                if (contains(screen, "LOGON REJECTED")) {
                    printScreen(screen, secret);
                    throw new IllegalStateException(
                            "TSO refused login. Password wasn't sent."
                    );
                }

                System.out.println("Sending password once.");
                passwordAttempted = true;
                terminal.command(passwordCommand);
                terminal.command("Enter()");

                System.out.println("Walking throught TSO i ISPF screens...");
                navigateToReady(terminal, secret);

                System.out.println("Starting MBKICKS...");
                List<String> before = terminal.screen();
                kicksLaunched = true;
                terminal.submit(START_KICKS);

                terminal.await(
                        s -> !s.equals(before) && !isBlank(s),
                        secret
                );

                prepareKicksStartup(terminal, secret);

                System.out.println("Typing MBGW through Key...");
                terminal.submitTransaction("MBGW");

                screen = terminal.await(
                        ManualTerminalProbe::isMonibankMap, secret
                );
                mapReached = true;

                System.out.println("=== MBGW ===");
                printScreen(screen, secret);
                System.out.println(
                        "OK: Java opened MONIBANK API."
                );
                System.out.println(
                        "Didn't send any bank operations."
                );

            } finally {
                // Sprzatanie nie moze zastapic glownego bledu testu.
                if (passwordAttempted && terminal.usable) {
                    try {
                        List<String> current = terminal.screen();

                        if (isLogon(current)) {
                            loggedOff = true;
                        } else {
                            if (kicksLaunched) {
                                // Zamykamy KICKS tylko przy rozpoznanym
                                // formularzu lub ekranie KICKS.
                                if (isMonibankMap(current)
                                        || looksLikeKicks(current)
                                        || (mapReached && isBlank(current))) {
                                    System.out.println("Closing KICKS...");
                                    prepareKicksCommandLine(terminal, secret);
                                    terminal.submitTransaction("KSSF");
                                } else if (!isReady(current)) {
                                    throw new IllegalStateException(
                                            "Unknown KICKS state. "
                                                    + "I don't send KSSF blindly."
                                    );
                                }
                            }

                            navigateToReady(terminal, secret);

                            System.out.println("Logging off TSO...");
                            terminal.submit("LOGOFF");
                            terminal.await(
                                    ManualTerminalProbe::isLogon, secret
                            );
                            loggedOff = true;
                            System.out.println(
                                    "OK: potwierdzono powrot do ekranu Logon."
                            );
                        }
                    } catch (Exception cleanupError) {
                        if (cleanupError instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        System.err.println(
                                "Logoff isn't confirmed. "
                                        + "HERC01 can still be an active session."
                        );
                    }
                }

                if (passwordAttempted && !loggedOff) {
                    System.err.println(
                            "WARNING: close the emulator isn't "
                                    + "TSO logoff confirmation."
                    );
                }
            }

            if (!mapReached || !loggedOff) {
                throw new IllegalStateException(
                        "Test failed: "
                                + "form MBGW i confirmed LOGOFF."
                );
            }

            System.out.println(
                    "OK: MBGW opened, KICKS Closed, TSO logoff."
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private static final String TEST_REQUEST_ID = "TEST0001";
    private static final String EXISTING_CUSTOMER_ID = "C000000000006";

    private static void executeGetCustomer(
            ProbeEmulator terminal,
            Pattern secret
    ) throws InterruptedException {
        System.out.println("Filling MBGW for GETCUST...");

        terminal.command("Wait(15,Unlock)");

        // Po wyświetleniu mapy kursor powinien być w polu OPERATION.
        terminal.typeKeys("GETCUST");

        terminal.command("Tab()");
        terminal.typeKeys(TEST_REQUEST_ID);

        terminal.command("Tab()");
        terminal.typeKeys(
                String.format("%04d", EXISTING_CUSTOMER_ID.length())
        );

        terminal.command("Tab()");
        terminal.typeKeys(EXISTING_CUSTOMER_ID);

        System.out.println("Executing GETCUST...");
        terminal.command("Enter()");

        List<String> result = terminal.await(
                screen -> contains(screen, "SUCCESS")
                        && contains(screen, TEST_REQUEST_ID)
                        && contains(screen, EXISTING_CUSTOMER_ID)
                        && contains(screen, ";A;OK"),
                secret
        );

        System.out.println("=== GETCUST RESULT ===");
        printScreen(result, secret);

        System.out.println(
                "OK: GETCUST returned existing customer "
                        + EXISTING_CUSTOMER_ID
        );
    }

    void typeKeys(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(
                    "Text entered with Key() cannot be empty."
            );
        }

        command("Wait(15,Unlock)");

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if (!Character.isLetterOrDigit(character)) {
                throw new IllegalArgumentException(
                        "Unsupported Key() character at position " + i
                );
            }

            command("Key(" + Character.toUpperCase(character) + ")");
        }
    }

    private static void navigateToReady(
            ProbeEmulator terminal, Pattern secret
    ) throws InterruptedException {
        List<String> screen = terminal.screen();

        for (int step = 0; step < 8; step++) {
            if (isReady(screen)) {
                return;
            }

            if (isLogon(screen)) {
                throw new IllegalStateException(
                        "Session came back to LOGON instead of READY."
                );
            }

            boolean listNotice = screen.stream().anyMatch(
                    line -> line.trim().matches(
                            "CLST020\\s+LIST data set not allocated"
                    )
            );

            if (listNotice) {
                System.out.println("LIST communicate: Enter, waiting for READY.");
                terminal.command("Wait(15,Unlock)");
                terminal.command("Enter()");

                // Jeden Enter. Nie ponawiamy go w petli.
                terminal.await(ManualTerminalProbe::isReady, secret);
                return;
            }

            List<String> before = screen;

            if (isIspf(screen)) {
                System.out.println("Menu ISPF: I type =X.");
                terminal.command("Wait(15,InputField)");
                terminal.command("Home()");
                terminal.command("EraseEOF()");
                terminal.submit("=X");
            } else if (lastText(screen).equals("***")) {
                System.out.println("Screen ***: Enter.");
                terminal.command("Wait(15,Unlock)");
                terminal.command("Enter()");
            } else {
                screen = terminal.await(
                        s -> isReady(s)
                                || isIspf(s)
                                || isLogon(s)
                                || lastText(s).equals("***")
                                || s.stream().anyMatch(
                                line -> line.trim().matches(
                                        "CLST020\\s+LIST data set not allocated"
                                )
                        ),
                        secret
                );
                continue;
            }

            screen = terminal.await(s -> !s.equals(before), secret);
        }

        printScreen(screen, secret);
        throw new IllegalStateException(
                "Didn't reach READY in allowed amount of steps."
        );
    }

    // Tylko start KICKS. Enter jest proba na pustym ekranie,
    // nigdy na formularzu bankowym. Nie wpisuje zadnej transakcji.
    static void prepareKicksStartup(
            ProbeEmulator terminal, Pattern secret
    ) throws InterruptedException {
        List<String> screen = terminal.screen();
        for (int step = 0; step < 8; step++) {
            if (isKicksWelcome(screen)) {
                break;
            }
            if (isReady(screen) || isIspf(screen) || isLoginEntry(screen)) {
                throw new IllegalStateException("KICKS didnt reach the start screen.");
            }
            if (lastText(screen).equals("***")) {
                System.out.println(" KICKS communicates: Enter.");
                List<String> before = screen;
                terminal.command("Wait(15,Unlock)");
                terminal.command("Enter()");
                screen = terminal.awaitScreen(s -> !s.equals(before), secret);
            } else {
                screen = terminal.awaitScreen(
                        s -> isKicksWelcome(s) || lastText(s).equals("***")
                                || isReady(s) || isIspf(s) || isLoginEntry(s),
                        secret
                );
            }
        }
        if (!isKicksWelcome(screen)) {
            printScreen(screen, secret);
            throw new IllegalStateException("Didnt recognize welcoming screen for KICKS.");
        }

        System.out.println(" KICKS welcoming screen: Clear.");
        terminal.command("Wait(15,Unlock)");
        terminal.command("Clear()");

        awaitBlankKicksScreen(terminal, secret);

        System.out.println(
                "Empty screen for KICKS, Go to type MBGW using Key()"
        );
    }

    // Sprzatanie: opuszcza formularz, ale NIGDY nie uruchamia MBGW.
    static void prepareKicksCommandLine(
            ProbeEmulator terminal, Pattern secret
    ) throws InterruptedException {
        List<String> screen = terminal.screen();
        if (isKicksWelcome(screen)) {
            prepareKicksStartup(terminal, secret);
            return;
        }
        if (!isMonibankMap(screen) && !isBlank(screen)) {
            printScreen(screen, secret);
            throw new IllegalStateException(
                    "Unknown screen forclosing KICKS. I didn't send any commands."
            );
        }
        System.out.println("Preparing screen for KSSF: Clear.");
        terminal.command("Wait(15,Unlock)");
        terminal.command("Clear()");
        awaitBlankKicksScreen(terminal, secret);
    }

    private static void awaitBlankKicksScreen(
            ProbeEmulator terminal, Pattern secret
    ) throws InterruptedException {
        terminal.await(ManualTerminalProbe::isBlank, secret);
        // Dodatkowy odczyt; sama pustka nie dowodzi mozliwosci pisania.
        Thread.sleep(250);
        List<String> screen = terminal.screen();
        if (!isBlank(screen)) {
            printScreen(screen, secret);
            throw new IllegalStateException("Screen isnt empty after clearing.");
        }
    }

    private static boolean isKicksWelcome(List<String> screen) {
        return contains(screen, "KSGM for tso user")
                && contains(screen, "CLEAR to continue");
    }

    private static boolean isMonibankMap(List<String> screen) {
        return contains(screen, "MONIBANK API")
                && contains(screen, "OPERATION:")
                && contains(screen, "REQUEST ID:")
                && contains(screen, "INPUT LENGTH:");
    }

    private static boolean isLoginEntry(List<String> screen) {
        return isLogon(screen)
                || screen.stream().anyMatch(
                line -> line.trim().equals("INPUT NOT RECOGNIZED")
        );
    }

    private static boolean looksLikeKicks(List<String> screen) {
        return contains(screen, "KICKS")
                || contains(screen, "K I C K S");
    }

    private static boolean isIspf(List<String> screen) {
        return contains(screen, "ISPF primary option menu")
                && contains(screen, "Option  ===>");
    }

    private static boolean isHerculesWelcome(List<String> screen) {
        return contains(screen, "Hercules Version")
                && contains(screen, "Device number");
    }

    private static boolean isLogon(List<String> screen) {
        return contains(screen, "Logon ===>");
    }

    private static boolean isReady(List<String> screen) {
        return lastText(screen).equals("READY");
    }

    private static boolean isBlank(List<String> screen) {
        return !screen.isEmpty()
                && screen.stream().allMatch(String::isBlank);
    }

    private static boolean contains(List<String> screen, String text) {
        return screen.stream().anyMatch(line -> line.contains(text));
    }

    private static String lastText(List<String> screen) {
        for (int i = screen.size() - 1; i >= 0; i--) {
            String line = screen.get(i).trim();
            if (!line.isEmpty()) {
                return line;
            }
        }
        return "";
    }

    private static void printScreen(List<String> screen, Pattern secret) {
        for (int row = 0; row < screen.size(); row++) {
            String safe = secret.matcher(screen.get(row))
                    .replaceAll("[UKRYTE]");
            System.out.printf("%02d | %s%n", row + 1, safe);
        }
    }

    private static String stringCommand(String text) {
        StringBuilder command = new StringBuilder("String(\"");

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character < 32 || character > 126) {
                throw new IllegalArgumentException(
                        "Only printable ASCII signs."
                );
            }
            command.append(String.format("\\u%04x", (int) character));
        }

        return command.append("\")").toString();
    }

    static final class CommandRejectedException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        CommandRejectedException(String message) {
            super(message);
        }
    }

    static class ProbeEmulator extends Emulator {

        private Pattern secret = Pattern.compile("(?!)");

        private boolean usable;

        ProbeEmulator(int port, ExecutorService executor) {
            super(port, executor);
        }

        List<String> screen() {
            return command("Ascii()");
        }

        void submit(String text) {
            command("Wait(15,Unlock)");
            command(stringCommand(text));
            command("Enter()");
        }

        // Wylacznie identyfikatory transakcji; nie sluzy do hasel.
        void submitTransaction(String transaction) {
            if (!transaction.equals("MBGW") && !transaction.equals("KSSF")) {
                throw new IllegalArgumentException("Unknown transaction.");
            }
            command("Wait(15,Unlock)");
            for (int i = 0; i < transaction.length(); i++) {
                command("Key(" + transaction.charAt(i) + ")");
            }

            // Nie naciskamy Enter, jesli nie potwierdzimy wszystkich liter.
            List<String> typed = screen();
            if (typed.stream().noneMatch(line -> line.trim().equals(transaction))) {
                throw new IllegalStateException(
                        "no confirmation for typing " + transaction
                                + ". No Enter command."
                );
            }
            command("Enter()");
        }

        List<String> awaitScreen(
                Predicate<List<String>> expected, Pattern secret
        ) throws InterruptedException {
            return awaitInternal(expected, secret, false);
        }

        List<String> await(
                Predicate<List<String>> expected, Pattern secret
        ) throws InterruptedException {
            return awaitInternal(expected, secret, true);
        }

        private List<String> awaitInternal(
                Predicate<List<String>> expected, Pattern secret,
                boolean waitForUnlock
        ) throws InterruptedException {
            long deadline = System.nanoTime()
                    + Duration.ofSeconds(20).toNanos();

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

            System.out.println("=== SCREEN MEANWHILE FAILure ===");
            printScreen(current, secret);
            throw new IllegalStateException(
                    "Expected screen didnt appear."
            );
        }

        List<String> command(String command) {
            return this.<List<String>>execute((writer, reader) -> {
                List<String> data = new ArrayList<>();
                String status = null;
                long deadline = System.nanoTime()
                        + Duration.ofSeconds(25).toNanos();

                try {
                    writer.write(command);
                    writer.write("\n");
                    writer.flush();

                    for (int count = 0; count < 256; count++) {
                        String line = readLine(reader, deadline);

                        if (line.startsWith("data: ")) {
                            data.add(line.substring(6));
                        } else if ("ok".equals(line)) {
                            if (status == null) {
                                throw new IOException("Brak statusu.");
                            }
                            usable = true;
                            return List.copyOf(data);
                        } else if ("error".equals(line)) {
                            // Koniec odpowiedzi zostal odczytany.
                            // Nie ujawniamy komendy ani danych z haslem.
                            usable = true;
                            int bracket = command.indexOf('(');
                            String action = bracket > 0
                                    ? command.substring(0, bracket) : "unknown";
                            // Dane ujawniamy tylko dla niesekretnych klawiszy.
                            boolean diagnostic = command.matches("Key\\([A-Z]\\)")
                                    || command.equals("Enter()")
                                    || command.equals("Clear()");
                            String details = diagnostic
                                    ? secret.matcher(String.join(" | ", data))
                                    .replaceAll("[HIDDEN]")
                                    : "Argumenty i odpowiedz ukryto.";
                            throw new CommandRejectedException(
                                    "Emulator odrzucil akcje: " + action
                                            + ". " + details + " Status: " + status
                            );
                        } else {
                            if (status != null) {
                                throw new IOException(
                                        "Nieoczekiwana struktura odpowiedzi."
                                );
                            }
                            status = line;
                        }
                    }

                    throw new IOException("Przekroczono limit linii.");
                } catch (IOException e) {
                    // Po timeout nie wykorzystujemy rozjechanego strumienia.
                    usable = false;
                    throw new UncheckedIOException(
                            "Communication with emulator failed.", e
                    );
                }
            });
        }

        private static String readLine(
                BufferedReader reader, long deadline
        ) throws IOException {
            StringBuilder line = new StringBuilder();

            while (System.nanoTime() < deadline) {
                if (reader.ready()) {
                    int character = reader.read();
                    if (character == -1) {
                        throw new IOException("Emulator closed the connection.");
                    }
                    if (character == '\n') {
                        return line.toString();
                    }
                    if (character != '\r') {
                        line.append((char) character);
                    }
                    if (line.length() > 16384) {
                        throw new IOException("Too long line.");
                    }
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted.", e);
                    }
                }
            }

            throw new IOException("Timeout emulator answer: 25 sekund.");
        }
    }
}
