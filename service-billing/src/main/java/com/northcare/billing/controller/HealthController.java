package com.northcare.billing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@Tag(name = "Health", description = "Kubernetes liveness and readiness probes")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Liveness probe — service is alive")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "billing-service",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness probe — service is ready to receive traffic")
    public ResponseEntity<Map<String, Object>> ready() {
        return ResponseEntity.ok(Map.of(
                "status", "READY",
                "service", "billing-service",
                "timestamp", Instant.now().toString()
        ));
    }
}
