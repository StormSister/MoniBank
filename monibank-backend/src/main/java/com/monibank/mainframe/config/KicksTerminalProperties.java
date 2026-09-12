package com.monibank.mainframe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "monibank.mainframe.terminal")
public record KicksTerminalProperties(
        boolean enabled,
        String host,
        int port,
        Duration recoveryDelay,
        int poolSize,
        List<KicksTerminalDefinition> sessions
) {
}
