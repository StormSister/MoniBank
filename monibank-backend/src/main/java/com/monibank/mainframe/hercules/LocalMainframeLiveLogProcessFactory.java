package com.monibank.mainframe.hercules;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.port.MainframeLiveLogProcessFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class LocalMainframeLiveLogProcessFactory
        implements MainframeLiveLogProcessFactory {

    private final MainframeProperties properties;

    @Override
    public Process start(int initialLines) throws IOException {

        return new ProcessBuilder(
                "tail",
                "-n", String.valueOf(initialLines),
                "-F", properties.liveLogPath()
        ).start();
    }
}
