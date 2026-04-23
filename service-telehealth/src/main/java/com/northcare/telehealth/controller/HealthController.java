package com.northcare.telehealth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Tag(name = "Health", description = "Liveness and readiness probes")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Liveness probe – service is running")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "telehealth",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness probe – service is ready to handle traffic")
    public ResponseEntity<Map<String, String>> ready() {
        return ResponseEntity.ok(Map.of(
                "status", "READY",
                "service", "telehealth"
        ));
    }
}
