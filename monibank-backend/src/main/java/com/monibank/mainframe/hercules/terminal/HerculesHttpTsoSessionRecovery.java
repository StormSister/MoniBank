package com.monibank.mainframe.hercules.terminal;

import com.monibank.mainframe.config.KicksTerminalDefinition;
import com.monibank.mainframe.config.KicksTerminalProperties;
import com.monibank.mainframe.config.MainframeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Component
public final class HerculesHttpTsoSessionRecovery
        implements TsoSessionRecovery {

    private static final Logger log = LoggerFactory.getLogger(
            HerculesHttpTsoSessionRecovery.class
    );
    private static final Pattern SAFE_USERID =
            Pattern.compile("MBKSRV[1-9][0-9]?");
    private static final Duration REQUEST_TIMEOUT =
            Duration.ofSeconds(5);

    private final MainframeProperties mainframeProperties;
    private final List<String> configuredUsernames;
    private final HttpClient httpClient;

    @Autowired
    public HerculesHttpTsoSessionRecovery(
            MainframeProperties mainframeProperties,
            KicksTerminalProperties terminalProperties
    ) {
        this(
                mainframeProperties,
                terminalProperties,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build()
        );
    }

    HerculesHttpTsoSessionRecovery(
            MainframeProperties mainframeProperties,
            KicksTerminalProperties terminalProperties,
            HttpClient httpClient
    ) {
        this.mainframeProperties = Objects.requireNonNull(
                mainframeProperties,
                "mainframeProperties cannot be null."
        );
        Objects.requireNonNull(
                terminalProperties,
                "terminalProperties cannot be null."
        );
        this.httpClient = Objects.requireNonNull(
                httpClient,
                "httpClient cannot be null."
        );

        List<KicksTerminalDefinition> sessions =
                terminalProperties.sessions();
        this.configuredUsernames = sessions == null
                ? List.of()
                : sessions.stream()
                .filter(Objects::nonNull)
                .map(KicksTerminalDefinition::username)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }

    @Override
    public boolean cancel(String username) {
        String safeUsername = validateUsername(username);
        String command = "/C U=" + safeUsername;
        String body = "command=" + encode(command) + "&send=Send";

        HttpRequest request = HttpRequest.newBuilder(operatorUri())
                .timeout(REQUEST_TIMEOUT)
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                )
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Hercules operator returned HTTP "
                                + response.statusCode() + "."
                );
            }

            log.warn(
                    "Requested cancellation of stuck TSO user {} through Hercules operator",
                    safeUsername
            );
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while cancelling stuck TSO user "
                            + safeUsername,
                    exception
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not reach Hercules operator at "
                            + operatorUri(),
                    exception
            );
        }
    }

    private String validateUsername(String username) {
        String normalized = Objects.requireNonNull(
                username,
                "username cannot be null."
        ).toUpperCase();

        if (!SAFE_USERID.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Unsafe TSO userid: " + normalized
            );
        }
        if (!configuredUsernames.contains(normalized)) {
            throw new IllegalArgumentException(
                    "TSO userid is not assigned to this terminal pool: "
                            + normalized
            );
        }

        return normalized;
    }

    private URI operatorUri() {
        String host = mainframeProperties.host();
        int port = mainframeProperties.httpPort();

        if (host == null || host.isBlank()) {
            throw new IllegalStateException(
                    "Missing Hercules HTTP host."
            );
        }
        if (port < 1 || port > 65535) {
            throw new IllegalStateException(
                    "Invalid Hercules HTTP port: " + port
            );
        }

        return URI.create(
                "http://" + host + ":" + port
                        + "/cgi-bin/tasks/syslog"
        );
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
