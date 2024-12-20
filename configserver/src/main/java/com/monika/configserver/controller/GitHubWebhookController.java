package com.monika.configserver.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class GitHubWebhookController {

    private final RestTemplate restTemplate;

    public GitHubWebhookController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/monitor")
    public ResponseEntity<Void> triggerRefresh() {
        String busRefreshUrl = "http://localhost:8080/actuator/busrefresh";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        restTemplate.exchange(busRefreshUrl, HttpMethod.POST, entity, Void.class);

        return ResponseEntity.ok().build();
    }
}

