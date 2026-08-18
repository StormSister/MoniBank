package com.monibank.mainframe.hercules;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.port.MainframeGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class HerculesMainframeGateway implements MainframeGateway {

    private final MainframeProperties properties;

    @Override
    public boolean isAvailable() {

        if (properties.host() == null || properties.host().isBlank()) {
            return false;
        }

        try (Socket socket = new Socket()) {

            socket.connect(
                    new InetSocketAddress(
                            properties.host(),
                            properties.readerPort()
                    ),
                    3_000
            );

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String submitJcl(String jcl) {
        try (Socket socket = new Socket()) {

            socket.connect(
                    new InetSocketAddress(
                            properties.host(),
                            properties.readerPort()
                    ),
                    5_000
            );

            try (OutputStream output = socket.getOutputStream()) {
                output.write(jcl.getBytes(StandardCharsets.US_ASCII));
                output.flush();
            }

            return "SUBMITTED";

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not submit JCL to Hercules at "
                            + properties.host()
                            + ":"
                            + properties.readerPort(),
                    e
            );
        }
    }
}