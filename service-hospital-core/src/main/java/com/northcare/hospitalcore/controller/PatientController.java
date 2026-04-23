package com.northcare.hospitalcore.controller;

import com.northcare.hospitalcore.dto.PatientRequest;
import com.northcare.hospitalcore.dto.PatientResponse;
import com.northcare.hospitalcore.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "HIPAA-compliant patient record management")
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    @Operation(summary = "List all active patients (paginated)")
    @ApiResponse(responseCode = "200", description = "Page of patient records")
    public ResponseEntity<Page<PatientResponse>> getAllPatients(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(patientService.getAllPatients(page, Math.min(size, 100)));
    }

    @PostMapping
    @Operation(summary = "Admit a new patient")
    @ApiResponse(responseCode = "201", description = "Patient created")
    @ApiResponse(responseCode = "422", description = "Validation failed")
    public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody PatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.createPatient(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    @ApiResponse(responseCode = "200", description = "Patient found")
    @ApiResponse(responseCode = "404", description = "Patient not found")
    public ResponseEntity<PatientResponse> getPatient(
            @Parameter(description = "Patient UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient record")
    @ApiResponse(responseCode = "200", description = "Patient updated")
    @ApiResponse(responseCode = "404", description = "Patient not found")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(patientService.updatePatient(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete (discharge) a patient")
    @ApiResponse(responseCode = "204", description = "Patient deactivated")
    @ApiResponse(responseCode = "404", description = "Patient not found")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
