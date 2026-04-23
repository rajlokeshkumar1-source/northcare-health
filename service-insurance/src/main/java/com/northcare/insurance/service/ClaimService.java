package com.northcare.insurance.service;

import com.northcare.insurance.dto.AppealClaimRequest;
import com.northcare.insurance.dto.ApproveClaimRequest;
import com.northcare.insurance.dto.ClaimRequest;
import com.northcare.insurance.dto.DenyClaimRequest;
import com.northcare.insurance.exception.BusinessException;
import com.northcare.insurance.exception.ResourceNotFoundException;
import com.northcare.insurance.model.Claim;
import com.northcare.insurance.model.Claim.ClaimStatus;
import com.northcare.insurance.model.InsurancePolicy;
import com.northcare.insurance.model.InsurancePolicy.PolicyStatus;
import com.northcare.insurance.repository.ClaimRepository;
import com.northcare.insurance.repository.InsurancePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final InsurancePolicyRepository policyRepository;

    @Transactional
    public Claim submitClaim(ClaimRequest request) {
        InsurancePolicy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + request.getPolicyId()));

        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new BusinessException("Claims can only be submitted against ACTIVE policies. Current status: " + policy.getStatus());
        }

        if (LocalDate.now().isAfter(policy.getCoverageEndDate())) {
            throw new BusinessException("Policy coverage period has expired on " + policy.getCoverageEndDate());
        }

        String claimNumber = generateClaimNumber();

        Claim claim = Claim.builder()
                .claimNumber(claimNumber)
                .policy(policy)
                .patientId(request.getPatientId())
                .invoiceId(request.getInvoiceId())
                .claimDate(LocalDate.now())
                .serviceDate(request.getServiceDate())
                .diagnosisCodes(request.getDiagnosisCodes())
                .procedureCodes(request.getProcedureCodes())
                .billedAmount(request.getBilledAmount())
                .notes(request.getNotes())
                .status(ClaimStatus.SUBMITTED)
                .build();

        Claim saved = claimRepository.save(claim);
        log.info("Submitted claim {} for policy {} patient {}", claimNumber, policy.getPolicyNumber(), request.getPatientId());
        return saved;
    }

    @Transactional
    public Claim reviewClaim(UUID id) {
        Claim claim = getClaimById(id);
        if (claim.getStatus() != ClaimStatus.SUBMITTED) {
            throw new BusinessException("Only SUBMITTED claims can be moved to UNDER_REVIEW. Current: " + claim.getStatus());
        }
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        claim.setProcessedAt(LocalDateTime.now());
        log.info("Claim {} moved to UNDER_REVIEW", claim.getClaimNumber());
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim approveClaim(UUID id, ApproveClaimRequest request) {
        Claim claim = getClaimById(id);
        if (claim.getStatus() != ClaimStatus.UNDER_REVIEW && claim.getStatus() != ClaimStatus.APPEALED) {
            throw new BusinessException("Claim must be UNDER_REVIEW or APPEALED to approve. Current: " + claim.getStatus());
        }

        BigDecimal approvedAmount = request.getApprovedAmount();
        ClaimStatus newStatus = approvedAmount.compareTo(claim.getBilledAmount()) >= 0
                ? ClaimStatus.APPROVED
                : ClaimStatus.PARTIALLY_APPROVED;

        claim.setApprovedAmount(approvedAmount);
        claim.setStatus(newStatus);
        claim.setProcessedAt(LocalDateTime.now());
        if (request.getNotes() != null) claim.setNotes(request.getNotes());

        // Update policy covered amount
        InsurancePolicy policy = claim.getPolicy();
        policy.setCoveredAmount(policy.getCoveredAmount().add(approvedAmount));
        policyRepository.save(policy);

        log.info("Claim {} {} with approved amount {}", claim.getClaimNumber(), newStatus, approvedAmount);
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim denyClaim(UUID id, DenyClaimRequest request) {
        Claim claim = getClaimById(id);
        if (claim.getStatus() != ClaimStatus.UNDER_REVIEW && claim.getStatus() != ClaimStatus.APPEALED) {
            throw new BusinessException("Claim must be UNDER_REVIEW or APPEALED to deny. Current: " + claim.getStatus());
        }
        claim.setStatus(ClaimStatus.DENIED);
        claim.setDenialReason(request.getDenialReason());
        claim.setProcessedAt(LocalDateTime.now());
        if (request.getNotes() != null) claim.setNotes(request.getNotes());

        log.info("Claim {} denied: {}", claim.getClaimNumber(), request.getDenialReason());
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim appealClaim(UUID id, AppealClaimRequest request) {
        Claim claim = getClaimById(id);
        if (claim.getStatus() != ClaimStatus.DENIED && claim.getStatus() != ClaimStatus.PARTIALLY_APPROVED) {
            throw new BusinessException("Only DENIED or PARTIALLY_APPROVED claims can be appealed. Current: " + claim.getStatus());
        }
        claim.setStatus(ClaimStatus.APPEALED);
        claim.setNotes((claim.getNotes() != null ? claim.getNotes() + "\n" : "") + "Appeal: " + request.getReason());
        claim.setProcessedAt(LocalDateTime.now());

        log.info("Claim {} appealed", claim.getClaimNumber());
        return claimRepository.save(claim);
    }

    @Transactional
    public Claim markPaid(UUID id) {
        Claim claim = getClaimById(id);
        if (claim.getStatus() != ClaimStatus.APPROVED && claim.getStatus() != ClaimStatus.PARTIALLY_APPROVED) {
            throw new BusinessException("Only APPROVED or PARTIALLY_APPROVED claims can be marked as PAID. Current: " + claim.getStatus());
        }
        claim.setStatus(ClaimStatus.PAID);
        claim.setPaidAmount(claim.getApprovedAmount());
        claim.setProcessedAt(LocalDateTime.now());

        log.info("Claim {} marked as PAID", claim.getClaimNumber());
        return claimRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public Claim getClaimById(UUID id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Claim> getClaimsByPolicy(UUID policyId, Pageable pageable) {
        return claimRepository.findByPolicyId(policyId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Claim> getClaimsByPatient(UUID patientId, Pageable pageable) {
        return claimRepository.findByPatientId(patientId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Claim> getAllClaims(Pageable pageable) {
        return claimRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Claim> getClaimsByStatus(ClaimStatus status, Pageable pageable) {
        return claimRepository.findByStatus(status, pageable);
    }

    private String generateClaimNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        long count = claimRepository.count() + 1;
        return String.format("CLM-%s-%05d", year, count);
    }
}
