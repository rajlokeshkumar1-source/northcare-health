package com.northcare.hospitalcore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Health", description = "Service health and readiness probes")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    @Operation(summary = "Liveness probe — returns service status")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "hospital-core",
                "version", "1.0.0"
        ));
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness probe — checks DB connectivity")
    public ResponseEntity<Map<String, String>> ready() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(Map.of(
                    "status", "READY",
                    "db", "UP"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "NOT_READY",
                    "db", "DOWN",
                    "error", e.getMessage()
            ));
        }
    }
}
