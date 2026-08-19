package com.monibank.mainframe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monibank.mainframe")
public record MainframeProperties(
        String provider,
        String host,
        int readerPort,
        int consolePort,
        int httpPort,
        int resultPort,
        String logHost,
        String logUser,
        String logPath,
        String jobUser,
        String jobPassword,

        String resultHost

) {
}