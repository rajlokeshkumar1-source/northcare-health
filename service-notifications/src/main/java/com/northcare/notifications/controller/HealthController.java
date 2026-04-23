package com.northcare.notifications.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "notifications"
        ));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> ready() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(1);
            if (valid) {
                return ResponseEntity.ok(Map.of("status", "READY", "db", "connected"));
            }
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "NOT_READY", "db", "unhealthy"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "NOT_READY", "db", e.getMessage()));
        }
    }
}
