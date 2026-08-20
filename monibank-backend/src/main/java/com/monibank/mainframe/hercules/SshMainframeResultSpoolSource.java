package com.monibank.mainframe.hercules;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.port.MainframeResultSpoolSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

@Component
@Profile("local")
@RequiredArgsConstructor
public class SshMainframeResultSpoolSource
        implements MainframeResultSpoolSource {

    private final MainframeProperties properties;

    @Override
    public List<String> readRecentLines(
            int numberOfLines
    ) {

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        "ssh",
                        properties.logUser()
                                + "@"
                                + properties.logHost(),
                        "tail",
                        "-n",
                        String.valueOf(numberOfLines),
                        properties.resultSpoolPath()
                );

        processBuilder.redirectErrorStream(true);

        try {

            Process process =
                    processBuilder.start();

            try (
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            process.getInputStream()
                                    )
                            )
            ) {

                List<String> lines =
                        reader.lines().toList();

                int exitCode =
                        process.waitFor();

                if (exitCode != 0) {
                    throw new IllegalStateException(
                            "Could not read mainframe result spool over SSH"
                    );
                }

                return lines;
            }

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Could not start SSH process",
                    e
            );

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            throw new IllegalStateException(
                    "SSH result spool reading interrupted",
                    e
            );
        }
    }
}