package com.monibank.mainframe.config;

public record KicksTerminalDefinition(
        String id,
        int emulatorControlPort,
        String username,
        String password,
        String kicksStartupCommand
) {
}
