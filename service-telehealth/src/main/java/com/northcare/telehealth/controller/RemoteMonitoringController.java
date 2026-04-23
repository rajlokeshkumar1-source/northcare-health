package com.northcare.telehealth.controller;

import com.northcare.telehealth.dto.MonitoringRequest;
import com.northcare.telehealth.dto.MonitoringResponse;
import com.northcare.telehealth.service.RemoteMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
@Tag(name = "Remote Monitoring", description = "Remote patient monitoring data ingestion and retrieval")
public class RemoteMonitoringController {

    private final RemoteMonitoringService monitoringService;

    @PostMapping("/readings")
    @Operation(summary = "Ingest a new device reading (auto-flags alert if outside thresholds)")
    public ResponseEntity<MonitoringResponse> ingestReading(@Valid @RequestBody MonitoringRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(monitoringService.ingestReading(request));
    }

    @GetMapping("/patients/{patientId}/readings")
    @Operation(summary = "Get paginated monitoring history for a patient (newest first)")
    public ResponseEntity<Page<MonitoringResponse>> getReadings(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(monitoringService.getReadingsByPatient(patientId, page, size));
    }

    @GetMapping("/patients/{patientId}/alerts")
    @Operation(summary = "Get all alert readings for a patient")
    public ResponseEntity<List<MonitoringResponse>> getAlerts(@PathVariable UUID patientId) {
        return ResponseEntity.ok(monitoringService.getAlertsByPatient(patientId));
    }
}
