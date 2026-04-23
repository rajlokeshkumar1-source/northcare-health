package com.northcare.insurance.controller;

import com.northcare.insurance.dto.CoverageCheckRequest;
import com.northcare.insurance.dto.CoverageCheckResponse;
import com.northcare.insurance.dto.PolicyRequest;
import com.northcare.insurance.model.InsurancePolicy;
import com.northcare.insurance.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Insurance Policies", description = "Manage insurance policies")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @Operation(summary = "Create a new insurance policy")
    public ResponseEntity<InsurancePolicy> createPolicy(@Valid @RequestBody PolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createPolicy(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get policy by ID")
    public ResponseEntity<InsurancePolicy> getPolicy(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    @GetMapping
    @Operation(summary = "List all policies (paginated)")
    public ResponseEntity<Page<InsurancePolicy>> getAllPolicies(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(policyService.getAllPolicies(pageable));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all policies for a patient")
    public ResponseEntity<List<InsurancePolicy>> getPoliciesByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(policyService.getPoliciesByPatient(patientId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a policy")
    public ResponseEntity<InsurancePolicy> updatePolicy(@PathVariable UUID id,
                                                         @Valid @RequestBody PolicyRequest request) {
        return ResponseEntity.ok(policyService.updatePolicy(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a policy")
    public ResponseEntity<Void> cancelPolicy(@PathVariable UUID id) {
        policyService.cancelPolicy(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/suspend")
    @Operation(summary = "Suspend a policy")
    public ResponseEntity<InsurancePolicy> suspendPolicy(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.suspendPolicy(id));
    }

    @PutMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate a suspended policy")
    public ResponseEntity<InsurancePolicy> reactivatePolicy(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.reactivatePolicy(id));
    }

    @PostMapping("/coverage-check")
    @Operation(summary = "Check if a patient's policy covers a given amount")
    public ResponseEntity<CoverageCheckResponse> checkCoverage(@Valid @RequestBody CoverageCheckRequest request) {
        return ResponseEntity.ok(policyService.checkCoverage(request));
    }
}
