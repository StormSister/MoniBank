package com.monibank.mainframe.api;

import com.monibank.mainframe.hercules.HerculesLiveLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/mainframe/logs")
@RequiredArgsConstructor
public class MainframeLiveLogController {

    private final HerculesLiveLogService liveLogService;

    @GetMapping(
            path = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter stream(
            @RequestHeader(
                    value = "Last-Event-ID",
                    required = false
            )
            Long lastEventId,
            HttpServletResponse response
    ) {

        response.setHeader(
                HttpHeaders.CACHE_CONTROL,
                "no-cache, no-transform"
        );
        response.setHeader("X-Accel-Buffering", "no");

        return liveLogService.subscribe(lastEventId);
    }
}
