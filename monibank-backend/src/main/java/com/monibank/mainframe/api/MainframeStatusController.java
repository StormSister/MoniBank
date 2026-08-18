package com.monibank.mainframe.api;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.port.MainframeGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mainframe")
@RequiredArgsConstructor
public class MainframeStatusController {

    private final MainframeGateway mainframeGateway;
    private final MainframeProperties properties;

    @GetMapping("/status")
    public Map<String, Object> status() {
        boolean available = mainframeGateway.isAvailable();

        return Map.of(
                "provider", properties.provider(),
                "system", "MVS 3.8j",
                "host", properties.host(),
                "readerPort", properties.readerPort(),
                "status", available ? "ONLINE" : "OFFLINE"
        );
    }
}