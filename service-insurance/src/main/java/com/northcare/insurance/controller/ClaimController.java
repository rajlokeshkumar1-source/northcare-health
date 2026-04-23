package com.northcare.insurance.controller;

import com.northcare.insurance.dto.AppealClaimRequest;
import com.northcare.insurance.dto.ApproveClaimRequest;
import com.northcare.insurance.dto.ClaimRequest;
import com.northcare.insurance.dto.DenyClaimRequest;
import com.northcare.insurance.model.Claim;
import com.northcare.insurance.service.ClaimService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
@Tag(name = "Claims", description = "Insurance claims lifecycle management")
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    @Operation(summary = "Submit a new claim")
    public ResponseEntity<Claim> submitClaim(@Valid @RequestBody ClaimRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claimService.submitClaim(request));
    }

    @GetMapping
    @Operation(summary = "List all claims (paginated)")
    public ResponseEntity<Page<Claim>> getAllClaims(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(claimService.getAllClaims(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get claim by ID")
    public ResponseEntity<Claim> getClaim(@PathVariable UUID id) {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }

    @PutMapping("/{id}/review")
    @Operation(summary = "Move claim to UNDER_REVIEW status")
    public ResponseEntity<Claim> reviewClaim(@PathVariable UUID id) {
        return ResponseEntity.ok(claimService.reviewClaim(id));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve a claim with approved amount")
    public ResponseEntity<Claim> approveClaim(@PathVariable UUID id,
                                               @Valid @RequestBody ApproveClaimRequest request) {
        return ResponseEntity.ok(claimService.approveClaim(id, request));
    }

    @PutMapping("/{id}/deny")
    @Operation(summary = "Deny a claim with reason")
    public ResponseEntity<Claim> denyClaim(@PathVariable UUID id,
                                            @Valid @RequestBody DenyClaimRequest request) {
        return ResponseEntity.ok(claimService.denyClaim(id, request));
    }

    @PutMapping("/{id}/appeal")
    @Operation(summary = "Appeal a denied or partially approved claim")
    public ResponseEntity<Claim> appealClaim(@PathVariable UUID id,
                                              @Valid @RequestBody AppealClaimRequest request) {
        return ResponseEntity.ok(claimService.appealClaim(id, request));
    }

    @PutMapping("/{id}/pay")
    @Operation(summary = "Mark an approved claim as PAID")
    public ResponseEntity<Claim> markPaid(@PathVariable UUID id) {
        return ResponseEntity.ok(claimService.markPaid(id));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get all claims for a patient")
    public ResponseEntity<Page<Claim>> getClaimsByPatient(@PathVariable UUID patientId,
                                                           @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(claimService.getClaimsByPatient(patientId, pageable));
    }

    @GetMapping("/policy/{policyId}")
    @Operation(summary = "Get all claims for a policy")
    public ResponseEntity<Page<Claim>> getClaimsByPolicy(@PathVariable UUID policyId,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(claimService.getClaimsByPolicy(policyId, pageable));
    }
}
