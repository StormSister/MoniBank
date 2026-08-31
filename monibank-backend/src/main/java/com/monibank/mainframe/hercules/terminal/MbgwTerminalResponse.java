package com.monibank.mainframe.hercules.terminal;

import java.util.List;
import java.util.Objects;

public record MbgwTerminalResponse(
        String requestId,
        MbgwTerminalStatus status,
        List<String> screen
) {

    public MbgwTerminalResponse {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException(
                    "requestId cannot be blank."
            );
        }

        Objects.requireNonNull(
                status,
                "status cannot be null."
        );

        Objects.requireNonNull(
                screen,
                "screen cannot be null."
        );

        screen = List.copyOf(screen);
    }

    public boolean successful() {
        return status == MbgwTerminalStatus.SUCCESS;
    }
}