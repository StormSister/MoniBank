package com.monibank.mainframe.hercules.terminal;

import com.monibank.mainframe.config.KicksTerminalDefinition;
import com.monibank.mainframe.config.KicksTerminalProperties;
import org.springframework.stereotype.Component;

@Component
public final class DefaultKicksTerminalSessionFactory
        implements KicksTerminalSessionFactory {

    @Override
    public KicksTerminalConnection create(
            KicksTerminalProperties properties,
            KicksTerminalDefinition definition
    ) {
        return new KicksTerminalSession(properties, definition);
    }
}
