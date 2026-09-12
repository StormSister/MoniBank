package com.monibank.mainframe.hercules.terminal;

import java.time.Instant;

public record TerminalSessionSnapshot(
        String id,
        String username,
        TerminalSessionState state,
        String currentRequestId,
        long recoveryCount,
        Instant readySince,
        Instant lastFailureAt,
        String lastError
) {
}
