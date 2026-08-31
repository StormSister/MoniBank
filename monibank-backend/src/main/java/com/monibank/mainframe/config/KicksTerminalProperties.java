package com.monibank.mainframe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monibank.mainframe.terminal")
public record KicksTerminalProperties(
        boolean enabled,
        String host,
        int port,
        int emulatorControlPort,
        String username,
        String password,
        String kicksStartupCommand
) {
}