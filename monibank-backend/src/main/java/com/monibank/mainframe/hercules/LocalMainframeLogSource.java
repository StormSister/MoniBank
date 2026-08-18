package com.monibank.mainframe.hercules;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.port.MainframeLogSource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@Profile("prod")
public class LocalMainframeLogSource implements MainframeLogSource {

    private final MainframeProperties properties;

    public LocalMainframeLogSource(MainframeProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<String> readRecentLines(int numberOfLines) {
        try {
            List<String> all =
                    Files.readAllLines(
                            Path.of(properties.logPath())
                    );

            int from = Math.max(0, all.size() - numberOfLines);

            return all.subList(from, all.size());

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read mainframe log locally",
                    e
            );
        }
    }
}