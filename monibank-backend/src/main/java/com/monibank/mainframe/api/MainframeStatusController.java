package com.monibank.mainframe.api;

import com.monibank.mainframe.hercules.MainframeStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mainframe")
@RequiredArgsConstructor
public class MainframeStatusController {

    private final MainframeStatusService statusService;

    @GetMapping("/status")
    public MainframeStatusResponse status() {
        return statusService.current();
    }
}
