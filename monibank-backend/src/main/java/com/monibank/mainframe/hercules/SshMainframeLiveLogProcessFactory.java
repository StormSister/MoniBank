package com.monibank.mainframe.hercules;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.port.MainframeLiveLogProcessFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("local")
@RequiredArgsConstructor
public class SshMainframeLiveLogProcessFactory
        implements MainframeLiveLogProcessFactory {

    private final MainframeProperties properties;

    @Override
    public Process start(int initialLines) throws IOException {

        List<String> command = new ArrayList<>(List.of(
                "ssh",
                "-o", "BatchMode=yes",
                "-o", "ConnectTimeout=10",
                properties.logUser() + "@" + properties.logHost()
        ));

        if (properties.liveLogContainer() != null
                && !properties.liveLogContainer().isBlank()) {
            command.add("docker");
            command.add("exec");
            command.add(properties.liveLogContainer());
        }

        command.add("tail");
        command.add("-n");
        command.add(String.valueOf(initialLines));
        command.add("-F");
        command.add(properties.liveLogPath());

        return new ProcessBuilder(command).start();
    }
}
