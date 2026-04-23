package com.northcare.insurance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@Tag(name = "Health", description = "Service health and readiness endpoints")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Liveness probe")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "insurance",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness probe")
    public ResponseEntity<Map<String, Object>> ready() {
        return ResponseEntity.ok(Map.of(
                "status", "READY",
                "service", "insurance",
                "timestamp", Instant.now().toString()
        ));
    }
}
