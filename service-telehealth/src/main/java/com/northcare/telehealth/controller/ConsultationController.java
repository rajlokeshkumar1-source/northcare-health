package com.northcare.telehealth.controller;

import com.northcare.telehealth.dto.ConsultationRequest;
import com.northcare.telehealth.dto.ConsultationResponse;
import com.northcare.telehealth.model.ConsultationStatus;
import com.northcare.telehealth.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consultations")
@RequiredArgsConstructor
@Tag(name = "Consultations", description = "Video consultation scheduling and state management")
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping
    @Operation(summary = "Schedule a new consultation")
    public ResponseEntity<ConsultationResponse> schedule(@Valid @RequestBody ConsultationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consultationService.scheduleConsultation(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get consultation by ID — includes PHI doctor notes")
    public ResponseEntity<ConsultationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(consultationService.getConsultationById(id));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get paginated consultations for a patient — notes excluded (PHI)")
    public ResponseEntity<Page<ConsultationResponse>> getByPatient(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(consultationService.getConsultationsByPatient(patientId, page, size));
    }

    @GetMapping("/today")
    @Operation(summary = "Get all active consultations scheduled for today")
    public ResponseEntity<List<ConsultationResponse>> getToday() {
        return ResponseEntity.ok(consultationService.getTodayConsultations());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get paginated consultations filtered by status")
    public ResponseEntity<Page<ConsultationResponse>> getByStatus(
            @PathVariable ConsultationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(consultationService.getConsultationsByStatus(status, page, size));
    }

    @PatchMapping("/{id}/start")
    @Operation(summary = "Transition consultation SCHEDULED → IN_PROGRESS")
    public ResponseEntity<ConsultationResponse> start(@PathVariable UUID id) {
        return ResponseEntity.ok(consultationService.startConsultation(id));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Transition consultation IN_PROGRESS → COMPLETED with doctor notes (PHI)")
    public ResponseEntity<ConsultationResponse> complete(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(consultationService.completeConsultation(id,
                body.getOrDefault("notes", "")));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a consultation (any non-terminal state)")
    public ResponseEntity<ConsultationResponse> cancel(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(consultationService.cancelConsultation(id,
                body.getOrDefault("reason", "")));
    }
}
