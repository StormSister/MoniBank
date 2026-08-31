package com.monibank.mainframe;

import com.monibank.mainframe.config.KicksTerminalProperties;
import com.monibank.mainframe.hercules.terminal.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KicksTerminalSessionProbe {

    @Test
    void executesTwoRequestsInOneSession() throws Exception {
        String password = System.getenv("MB_TSO_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Missing MB_TSO_PASSWORD."
            );
        }

        String username = System.getenv("MB_TSO_USERNAME");

        if (username == null || username.isBlank()) {
            username = "HERC01";
        }

        String kicksCommand =
                System.getenv("MB_KICKS_STARTUP_COMMAND");

        if (kicksCommand == null || kicksCommand.isBlank()) {
            kicksCommand =
                    "EXEC '" + username + ".CMDPROC(MBKICKS)'";
        }

        KicksTerminalProperties properties =
                new KicksTerminalProperties(
                        true,
                        "127.0.0.1",
                        13271,
                        13270,
                        username,
                        password,
                        kicksCommand
                );

        try (KicksTerminalSession session =
                     new KicksTerminalSession(properties)) {

            session.open();

            assertEquals(
                    TerminalSessionState.READY,
                    session.state()
            );

            MbgwTerminalResponse success = session.execute(
                    new MbgwRequest(
                            "GETCUST",
                            "TEST0001",
                            "C000000000006"
                    )
            );

            assertEquals(
                    MbgwTerminalStatus.SUCCESS,
                    success.status()
            );
            assertTrue(success.successful());
            assertTrue(
                    contains(success, "C000000000006;A;OK")
            );
            assertEquals(
                    TerminalSessionState.READY,
                    session.state()
            );

            MbgwTerminalResponse notFound = session.execute(
                    new MbgwRequest(
                            "GETCUST",
                            "TEST0002",
                            "C000000000001"
                    )
            );

            assertEquals(
                    MbgwTerminalStatus.ERROR,
                    notFound.status()
            );
            assertTrue(
                    contains(notFound, "NOTFOUND")
            );
            assertEquals(
                    TerminalSessionState.READY,
                    session.state()
            );
        }
    }

    private static boolean contains(
            MbgwTerminalResponse response,
            String expected
    ) {
        return response.screen().stream()
                .anyMatch(line -> line.contains(expected));
    }
}