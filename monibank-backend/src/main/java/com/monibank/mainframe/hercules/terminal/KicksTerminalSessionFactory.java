package com.monibank.mainframe.hercules.terminal;

import com.monibank.mainframe.config.KicksTerminalDefinition;
import com.monibank.mainframe.config.KicksTerminalProperties;

@FunctionalInterface
public interface KicksTerminalSessionFactory {

    KicksTerminalConnection create(
            KicksTerminalProperties properties,
            KicksTerminalDefinition definition
    );
}
