package com.monibank.mainframe.hercules.terminal;

import com.monibank.mainframe.config.KicksTerminalDefinition;
import com.monibank.mainframe.config.KicksTerminalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class KicksTerminalSession
        implements KicksTerminalConnection {

    private static final Logger log =
            LoggerFactory.getLogger(KicksTerminalSession.class);

    private static final String TERMINAL_MODEL = "3279-2";
    private static final int MBGW_INPUT_FIELD_LENGTH = 64;
    private static final int MBGW_INPUT_FIRST_ROW = 7;
    private static final int MBGW_INPUT_COLUMN = 5;

    private final KicksTerminalProperties properties;
    private final KicksTerminalDefinition definition;
    private final ExecutorService executor;

    private J3270Terminal terminal;
    private TerminalSessionState state =
            TerminalSessionState.DISCONNECTED;

    public KicksTerminalSession(
            KicksTerminalProperties properties,
            KicksTerminalDefinition definition
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "properties cannot be null."
        );
        this.definition = Objects.requireNonNull(
                definition,
                "definition cannot be null."
        );
        this.executor = Executors.newSingleThreadExecutor();
    }

    public synchronized TerminalSessionState state() {
        return state;
    }

    public synchronized void open() throws Exception {
        requireState(TerminalSessionState.DISCONNECTED);
        validateConfiguration();

        terminal = new J3270Terminal(
                definition.emulatorControlPort(),
                executor
        );
        terminal.setVisible(false);
        terminal.setModel(TERMINAL_MODEL);
        terminal.protectSecret(definition.password());

        try {
            state = TerminalSessionState.CONNECTING;

            log.info("Starting 3270 emulator");
            terminal.start();

            log.info(
                    "Connecting terminal to {}:{}",
                    properties.host(),
                    properties.port()
            );
            terminal.command(
                    "Connect(" + properties.host()
                            + ":" + properties.port() + ")"
            );

            state = TerminalSessionState.LOGGING_IN;
            logIn();

            state = TerminalSessionState.STARTING_KICKS;
            startKicks();

            state = TerminalSessionState.OPENING_MBGW;
            openMbgw();

            state = TerminalSessionState.READY;
            log.info("MBGW terminal session is READY");
        } catch (Exception exception) {
            state = TerminalSessionState.FAILED;
            releaseResources();
            throw exception;
        }
    }

    private void typeMbgwInput(
            String input
    ) {

        int fieldNumber = 0;

        for (int offset = 0;
             offset < input.length();
             offset += MBGW_INPUT_FIELD_LENGTH) {

            int end =
                    Math.min(
                            offset + MBGW_INPUT_FIELD_LENGTH,
                            input.length()
                    );

            String chunk =
                    input.substring(
                            offset,
                            end
                    );

            terminal.typeTextAt(
                    MBGW_INPUT_FIRST_ROW + fieldNumber,
                    MBGW_INPUT_COLUMN,
                    chunk
            );

            fieldNumber++;
        }
    }

    public synchronized MbgwTerminalResponse execute(
            MbgwRequest request
    ) throws InterruptedException {
        Objects.requireNonNull(request, "request cannot be null.");
        requireState(TerminalSessionState.READY);

        state = TerminalSessionState.BUSY;

        try {
            log.info(
                    "Executing MBGW operation {} [{}]",
                    request.operation(),
                    request.requestId()
            );

            terminal.command("Wait(15,Unlock)");
            terminal.typeKeys(request.operation());

            terminal.command("Tab()");
            terminal.typeKeys(request.requestId());

            terminal.command("Tab()");
            terminal.typeKeys(request.formattedInputLength());

            typeMbgwInput(request.input());

            terminal.command("Enter()");

            List<String> resultScreen = terminal.await(
                    screen -> isCompletedMbgwResponse(screen)
                            && hasResultFor(
                            screen,
                            request.requestId()
                    )
            );

            MbgwTerminalStatus status = contains(
                    resultScreen,
                    "SUCCESS"
            )
                    ? MbgwTerminalStatus.SUCCESS
                    : MbgwTerminalStatus.ERROR;

            MbgwTerminalResponse response =
                    new MbgwTerminalResponse(
                            request.requestId(),
                            status,
                            resultScreen
                    );

            prepareMapForNextRequest(request.requestId());

            state = TerminalSessionState.READY;

            log.info(
                    "MBGW operation {} [{}] completed with {}",
                    request.operation(),
                    request.requestId(),
                    status
            );

            return response;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            state = TerminalSessionState.FAILED;
            throw exception;
        } catch (RuntimeException exception) {
            state = TerminalSessionState.FAILED;
            throw exception;
        }
    }

    @Override
    public synchronized void verifyReady() {
        requireState(TerminalSessionState.READY);

        if (terminal == null || !terminal.isUsable()) {
            state = TerminalSessionState.FAILED;
            throw new IllegalStateException(
                    "3270 emulator is not available."
            );
        }

        try {
            List<String> screen = terminal.screen();

            if (!isReadyMonibankMap(screen)) {
                terminal.printScreen(screen);
                throw new IllegalStateException(
                        "MBGW READY screen was not recognized during health check."
                );
            }
        } catch (RuntimeException exception) {
            state = TerminalSessionState.FAILED;
            throw exception;
        }
    }



    @Override
    public synchronized void close() throws Exception {
        if (state == TerminalSessionState.CLOSED) {
            return;
        }

        if (terminal == null) {
            releaseResources();
            state = TerminalSessionState.CLOSED;
            return;
        }

        state = TerminalSessionState.CLOSING;
        boolean logoffConfirmed = false;
        Exception closeFailure = null;

        try {
            if (terminal.isUsable()) {
                List<String> current = terminal.screen();

                if (isLogon(current)) {
                    logoffConfirmed = true;
                } else {
                    if (isMonibankMap(current)
                            || looksLikeKicks(current)
                            || isBlank(current)) {
                        log.info("Closing KICKS");
                        prepareKicksCommandLine();
                        terminal.submitTransaction("KSSF");
                    } else if (!isReady(current)) {
                        throw new IllegalStateException(
                                "Unknown terminal state. "
                                        + "KSSF was not sent blindly."
                        );
                    }

                    navigateToReady();

                    log.info("Logging off TSO user {}", definition.username());
                    terminal.submit("LOGOFF");
                    terminal.await(KicksTerminalSession::isLogon);
                    logoffConfirmed = true;
                }
            }
        } catch (Exception exception) {
            closeFailure = exception;
        } finally {
            releaseResources();
            state = logoffConfirmed
                    ? TerminalSessionState.CLOSED
                    : TerminalSessionState.FAILED;
        }

        if (closeFailure != null) {
            throw closeFailure;
        }

        if (!logoffConfirmed) {
            throw new IllegalStateException(
                    "TSO logoff was not confirmed for user "
                            + definition.username() + "."
            );
        }
    }

    private void logIn() throws InterruptedException {
        List<String> screen = terminal.awaitScreen(
                candidate -> isLoginEntry(candidate)
                        || isHerculesWelcome(candidate)
        );

        if (!isLoginEntry(screen) && isHerculesWelcome(screen)) {
            log.info("Hercules welcome screen: Enter");
            terminal.command("Enter()");
            screen = terminal.awaitScreen(
                    KicksTerminalSession::isLoginEntry
            );
        }

        terminal.command("Wait(15,Unlock)");

        log.info("Typing TSO user {}", definition.username());

        if (isLogon(screen)) {
            if (hasFormattedLogonField(screen)) {
                terminal.command("Wait(15,InputField)");
            } else {
                /*
                 * IKJ56700A ENTER USERID is a line-mode prompt. It has no
                 * 3270 input field, so Wait(InputField) can never succeed.
                 */
                terminal.command("Wait(15,Unlock)");
            }
            terminal.typeKeys(definition.username());
            terminal.command("Enter()");
        } else {
            terminal.command("Wait(15,Unlock)");
            terminal.typeKeys("LOGON");
            terminal.command("Key(space)");
            terminal.typeKeys(definition.username());
            terminal.command("Enter()");
        }

        String passwordPrompt =
                "ENTER CURRENT PASSWORD FOR "
                        + definition.username() + "-";

        screen = terminal.await(
                candidate -> contains(candidate, passwordPrompt)
                        || contains(candidate, "LOGON REJECTED")
        );

        if (contains(screen, "LOGON REJECTED")) {
            terminal.printScreen(screen);

            if (contains(screen, "IN USE")) {
                throw new TsoUserInUseException(
                        definition.username()
                );
            }

            throw new IllegalStateException(
                    "TSO rejected login for user "
                            + definition.username()
                            + ". Password was not sent."
            );
        }

        log.info("Sending TSO password once");
        terminal.submit(definition.password());

        navigateToReady();
    }

    private void startKicks() throws InterruptedException {
        log.info("Starting KICKS");

        List<String> before = terminal.screen();
        terminal.submit(definition.kicksStartupCommand());

        terminal.await(
                screen -> !screen.equals(before) && !isBlank(screen)
        );

        prepareKicksStartup();
    }

    private void openMbgw() throws InterruptedException {
        log.info("Typing MBGW through Key()");
        terminal.submitTransaction("MBGW");

        List<String> screen = terminal.await(
                KicksTerminalSession::isReadyMonibankMap
        );

        log.info("MBGW map opened");
        terminal.printScreen(screen);
    }

    private void prepareMapForNextRequest(
            String completedRequestId
    ) throws InterruptedException {
        terminal.command("Wait(15,Unlock)");
        terminal.command("Enter()");

        terminal.await(
                screen -> isReadyMonibankMap(screen)
                        && !contains(screen, completedRequestId)
        );
    }

    private void navigateToReady() throws InterruptedException {
        List<String> screen = terminal.screen();

        for (int step = 0; step < 8; step++) {
            if (isReady(screen)) {
                return;
            }

            if (isLogon(screen)) {
                throw new IllegalStateException(
                        "Session returned to LOGON instead of READY."
                );
            }

            if (hasListNotice(screen)) {
                log.info("LIST notice: Enter, waiting for READY");
                terminal.command("Wait(15,Unlock)");
                terminal.command("Enter()");
                terminal.await(KicksTerminalSession::isReady);
                return;
            }

            List<String> before = screen;

            if (isIspf(screen)) {
                log.info("ISPF menu: typing =X");
                terminal.command("Wait(15,InputField)");
                terminal.command("Home()");
                terminal.command("EraseEOF()");
                terminal.submit("=X");
            } else if ("***".equals(lastText(screen))) {
                log.info("TSO message screen: Enter");
                terminal.command("Wait(15,Unlock)");
                terminal.command("Enter()");
            } else {
                screen = terminal.await(
                        candidate -> isReady(candidate)
                                || isIspf(candidate)
                                || isLogon(candidate)
                                || "***".equals(lastText(candidate))
                                || hasListNotice(candidate)
                );
                continue;
            }

            screen = terminal.await(
                    candidate -> !candidate.equals(before)
            );
        }

        terminal.printScreen(screen);
        throw new IllegalStateException(
                "READY was not reached in the allowed number of steps."
        );
    }

    private void prepareKicksStartup() throws InterruptedException {
        List<String> screen = terminal.screen();

        for (int step = 0; step < 8; step++) {
            if (isKicksWelcome(screen)) {
                break;
            }

            if (isReady(screen)
                    || isIspf(screen)
                    || isLoginEntry(screen)) {
                throw new IllegalStateException(
                        "KICKS did not reach its welcome screen."
                );
            }

            if ("***".equals(lastText(screen))) {
                log.info("KICKS message screen: Enter");
                List<String> before = screen;
                terminal.command("Wait(15,Unlock)");
                terminal.command("Enter()");
                screen = terminal.awaitScreen(
                        candidate -> !candidate.equals(before)
                );
            } else {
                screen = terminal.awaitScreen(
                        candidate -> isKicksWelcome(candidate)
                                || "***".equals(lastText(candidate))
                                || isReady(candidate)
                                || isIspf(candidate)
                                || isLoginEntry(candidate)
                );
            }
        }

        if (!isKicksWelcome(screen)) {
            terminal.printScreen(screen);
            throw new IllegalStateException(
                    "KICKS welcome screen was not recognized."
            );
        }

        log.info("KICKS welcome screen: Clear");
        clearKicksWelcomeScreen();
    }

    private void clearKicksWelcomeScreen()
            throws InterruptedException {

        for (int attempt = 1; attempt <= 3; attempt++) {
            terminal.command("Wait(15,Unlock)");
            terminal.command("Clear()");

            Thread.sleep(750);

            List<String> screen = terminal.screen();

            if (isBlank(screen)) {
                return;
            }

            if (!isKicksWelcome(screen)) {
                terminal.printScreen(screen);
                throw new IllegalStateException(
                        "Unexpected screen appeared after Clear."
                );
            }

            log.warn(
                    "KICKS welcome screen remained after Clear. "
                            + "Retry {}/3",
                    attempt
            );
        }

        List<String> screen = terminal.screen();
        terminal.printScreen(screen);

        throw new IllegalStateException(
                "KICKS welcome screen did not react "
                        + "to Clear after 3 attempts."
        );
    }

    private void prepareKicksCommandLine()
            throws InterruptedException {
        List<String> screen = terminal.screen();

        if (isKicksWelcome(screen)) {
            prepareKicksStartup();
            return;
        }

        if (!isMonibankMap(screen) && !isBlank(screen)) {
            terminal.printScreen(screen);
            throw new IllegalStateException(
                    "Unknown KICKS screen. No closing command was sent."
            );
        }

        log.info("Preparing command line for KSSF: Clear");
        terminal.command("Wait(15,Unlock)");
        terminal.command("Clear()");
        awaitBlankKicksScreen();
    }

    private void awaitBlankKicksScreen()
            throws InterruptedException {
        terminal.await(KicksTerminalSession::isBlank);
        Thread.sleep(250);

        List<String> screen = terminal.screen();

        if (!isBlank(screen)) {
            terminal.printScreen(screen);
            throw new IllegalStateException(
                    "KICKS screen is not blank after Clear."
            );
        }
    }

    private void validateConfiguration() {
        if (!properties.enabled()) {
            throw new IllegalStateException(
                    "KICKS terminal integration is disabled."
            );
        }

        requireText("host", properties.host());
        requirePort("port", properties.port());
        requirePort(
                "emulatorControlPort",
                definition.emulatorControlPort()
        );
        requireText("id", definition.id());
        requireText("username", definition.username());
        requireText("password", definition.password());
        requireText(
                "kicksStartupCommand",
                definition.kicksStartupCommand()
        );
    }

    private void requireState(TerminalSessionState expected) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Terminal state must be " + expected
                            + ", but was " + state + "."
            );
        }
    }

    private void releaseResources() {
        J3270Terminal currentTerminal = terminal;
        terminal = null;

        if (currentTerminal != null) {
            try {
                currentTerminal.close();
            } catch (Exception exception) {
                log.warn(
                        "Could not close 3270 emulator cleanly",
                        exception
                );
            }
        }

        executor.shutdownNow();
    }

    private static void requireText(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing terminal configuration: " + name
            );
        }
    }

    private static void requirePort(String name, int value) {
        if (value < 1 || value > 65535) {
            throw new IllegalStateException(
                    "Invalid terminal port " + name + ": " + value
            );
        }
    }

    private static boolean isCompletedMbgwResponse(
            List<String> screen
    ) {
        return isMonibankMap(screen)
                && (contains(screen, "SUCCESS")
                || contains(screen, "ERROR"));
    }

    private static boolean isReadyMonibankMap(
            List<String> screen
    ) {
        return isMonibankMap(screen)
                && contains(screen, "READY")
                && !contains(screen, "SUCCESS")
                && !contains(screen, "ERROR");
    }

    private static boolean isMonibankMap(List<String> screen) {
        return contains(screen, "MONIBANK API")
                && contains(screen, "OPERATION:")
                && contains(screen, "REQUEST ID:")
                && contains(screen, "INPUT LENGTH:");
    }

    private static boolean hasResultFor(
            List<String> screen,
            String requestId
    ) {
        return screen.stream().anyMatch(
                line -> line.contains("RESULT FOR:")
                        && line.contains(requestId)
        );
    }

    private static boolean hasListNotice(List<String> screen) {
        return screen.stream().anyMatch(
                line -> line.trim().matches(
                        "CLST020\\s+LIST data set not allocated"
                )
        );
    }

    private static boolean isLoginEntry(List<String> screen) {
        return isLogon(screen)
                || screen.stream().anyMatch(
                line -> line.trim().equals("INPUT NOT RECOGNIZED")
        );
    }

    private static boolean isHerculesWelcome(List<String> screen) {
        return contains(screen, "Hercules Version")
                && contains(screen, "Device number");
    }

    private static boolean isKicksWelcome(List<String> screen) {
        return contains(screen, "KSGM for tso user")
                && contains(screen, "CLEAR to continue");
    }

    private static boolean looksLikeKicks(List<String> screen) {
        return contains(screen, "KICKS")
                || contains(screen, "K I C K S");
    }

    private static boolean isIspf(List<String> screen) {
        return contains(screen, "ISPF primary option menu")
                && contains(screen, "Option  ===>");
    }

    private static boolean isLogon(List<String> screen) {
        return hasFormattedLogonField(screen)
                || contains(
                screen,
                "IKJ56700A ENTER USERID"
        );
    }

    private static boolean hasFormattedLogonField(
            List<String> screen
    ) {
        return contains(screen, "Logon ===>");
    }

    private static boolean isReady(List<String> screen) {
        return "READY".equals(lastText(screen));
    }

    private static boolean isBlank(List<String> screen) {
        return !screen.isEmpty()
                && screen.stream().allMatch(String::isBlank);
    }

    private static boolean contains(
            List<String> screen,
            String text
    ) {
        return screen.stream().anyMatch(
                line -> line.contains(text)
        );
    }

    private static String lastText(List<String> screen) {
        for (int index = screen.size() - 1; index >= 0; index--) {
            String line = screen.get(index).trim();

            if (!line.isEmpty()) {
                return line;
            }
        }

        return "";
    }
}
