package com.monibank.mainframe;
import com.monibank.mainframe.hercules.terminal.HerculesHttpTsoSessionRecovery;
import com.monibank.mainframe.config.KicksTerminalDefinition;
import com.monibank.mainframe.config.KicksTerminalProperties;
import com.monibank.mainframe.config.MainframeProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HerculesHttpTsoSessionRecoveryTest {

    @Test
    void postsOnlyCancelCommandForConfiguredWorker()
            throws Exception {

        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );
        server.createContext(
                "/cgi-bin/tasks/syslog",
                exchange -> {
                    method.set(exchange.getRequestMethod());
                    body.set(new String(
                            exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8
                    ));
                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().close();
                }
        );
        server.start();

        try {
            HerculesHttpTsoSessionRecovery recovery =
                    new HerculesHttpTsoSessionRecovery(
                            mainframeProperties(server.getAddress().getPort()),
                            terminalProperties("MBKSRV1")
                    );

            assertTrue(recovery.cancel("MBKSRV1"));
            assertEquals("POST", method.get());
            assertEquals(
                    "command=%2FC+U%3DMBKSRV1&send=Send",
                    body.get()
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsNonWorkerUseridEvenWhenConfigured() {
        HerculesHttpTsoSessionRecovery recovery =
                new HerculesHttpTsoSessionRecovery(
                        mainframeProperties(8038),
                        terminalProperties("HERC01")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> recovery.cancel("HERC01")
        );
    }

    private static KicksTerminalProperties terminalProperties(
            String username
    ) {
        return new KicksTerminalProperties(
                true,
                "127.0.0.1",
                13271,
                Duration.ofSeconds(2),
                1,
                List.of(new KicksTerminalDefinition(
                        "SOFIA",
                        13270,
                        username,
                        "MONIBANK",
                        "EXEC 'HERC01.CMDPROC(MBKICKS)'"
                ))
        );
    }

    private static MainframeProperties mainframeProperties(
            int httpPort
    ) {
        return new MainframeProperties(
                "HERCULES",
                "127.0.0.1",
                "127.0.0.1",
                3505,
                3270,
                httpPort,
                5001,
                "127.0.0.1",
                "root",
                "/tmp/mainframe.log",
                "HERC01",
                "MONIBANK",
                "127.0.0.1",
                "/tmp/printer.txt",
                "monibank-mainframe",
                "/opt/mvs-tk5/log/hardcopy.log"
        );
    }
}
