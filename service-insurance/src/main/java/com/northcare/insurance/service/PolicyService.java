package com.northcare.insurance.service;

import com.northcare.insurance.dto.CoverageCheckRequest;
import com.northcare.insurance.dto.CoverageCheckResponse;
import com.northcare.insurance.dto.PolicyRequest;
import com.northcare.insurance.exception.BusinessException;
import com.northcare.insurance.exception.ResourceNotFoundException;
import com.northcare.insurance.model.InsurancePolicy;
import com.northcare.insurance.model.InsurancePolicy.PolicyStatus;
import com.northcare.insurance.repository.InsurancePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final InsurancePolicyRepository policyRepository;

    @Transactional
    public InsurancePolicy createPolicy(PolicyRequest request) {
        if (request.getCoverageEndDate().isBefore(request.getCoverageStartDate())) {
            throw new BusinessException("Coverage end date must be after start date");
        }

        String policyNumber = generatePolicyNumber();

        InsurancePolicy policy = InsurancePolicy.builder()
                .policyNumber(policyNumber)
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .providerName(request.getProviderName())
                .policyType(request.getPolicyType())
                .coverageStartDate(request.getCoverageStartDate())
                .coverageEndDate(request.getCoverageEndDate())
                .deductibleAmount(request.getDeductibleAmount() != null ? request.getDeductibleAmount() : BigDecimal.ZERO)
                .coverageLimit(request.getCoverageLimit())
                .groupNumber(request.getGroupNumber())
                .memberNumber(request.getMemberNumber())
                .status(PolicyStatus.ACTIVE)
                .isActive(true)
                .build();

        InsurancePolicy saved = policyRepository.save(policy);
        log.info("Created insurance policy {} for patient {}", policyNumber, request.getPatientId());
        return saved;
    }

    @Transactional(readOnly = true)
    public InsurancePolicy getPolicyById(UUID id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<InsurancePolicy> getAllPolicies(Pageable pageable) {
        return policyRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<InsurancePolicy> getPoliciesByPatient(UUID patientId) {
        return policyRepository.findByPatientId(patientId);
    }

    @Transactional
    public InsurancePolicy updatePolicy(UUID id, PolicyRequest request) {
        InsurancePolicy policy = getPolicyById(id);

        if (request.getCoverageEndDate().isBefore(request.getCoverageStartDate())) {
            throw new BusinessException("Coverage end date must be after start date");
        }

        policy.setProviderId(request.getProviderId());
        policy.setProviderName(request.getProviderName());
        policy.setPolicyType(request.getPolicyType());
        policy.setCoverageStartDate(request.getCoverageStartDate());
        policy.setCoverageEndDate(request.getCoverageEndDate());
        policy.setDeductibleAmount(request.getDeductibleAmount());
        policy.setCoverageLimit(request.getCoverageLimit());
        policy.setGroupNumber(request.getGroupNumber());
        policy.setMemberNumber(request.getMemberNumber());

        return policyRepository.save(policy);
    }

    @Transactional
    public void cancelPolicy(UUID id) {
        InsurancePolicy policy = getPolicyById(id);
        if (policy.getStatus() == PolicyStatus.CANCELLED) {
            throw new BusinessException("Policy is already cancelled");
        }
        policy.setStatus(PolicyStatus.CANCELLED);
        policy.setActive(false);
        policyRepository.save(policy);
        log.info("Cancelled policy {}", policy.getPolicyNumber());
    }

    @Transactional
    public InsurancePolicy suspendPolicy(UUID id) {
        InsurancePolicy policy = getPolicyById(id);
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new BusinessException("Only ACTIVE policies can be suspended");
        }
        policy.setStatus(PolicyStatus.SUSPENDED);
        return policyRepository.save(policy);
    }

    @Transactional
    public InsurancePolicy reactivatePolicy(UUID id) {
        InsurancePolicy policy = getPolicyById(id);
        if (policy.getStatus() != PolicyStatus.SUSPENDED) {
            throw new BusinessException("Only SUSPENDED policies can be reactivated");
        }
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setActive(true);
        return policyRepository.save(policy);
    }

    @Transactional(readOnly = true)
    public CoverageCheckResponse checkCoverage(CoverageCheckRequest request) {
        List<InsurancePolicy> activePolicies = policyRepository.findByPatientIdAndStatus(
                request.getPatientId(), PolicyStatus.ACTIVE);

        if (activePolicies.isEmpty()) {
            return CoverageCheckResponse.builder()
                    .patientId(request.getPatientId())
                    .covered(false)
                    .requestedAmount(request.getAmount())
                    .message("No active insurance policy found for patient")
                    .build();
        }

        InsurancePolicy policy = activePolicies.get(0);
        LocalDate today = LocalDate.now();

        if (today.isAfter(policy.getCoverageEndDate())) {
            return CoverageCheckResponse.builder()
                    .patientId(request.getPatientId())
                    .covered(false)
                    .policyNumber(policy.getPolicyNumber())
                    .policyStatus(policy.getStatus())
                    .requestedAmount(request.getAmount())
                    .coverageEndDate(policy.getCoverageEndDate())
                    .message("Policy coverage has expired")
                    .build();
        }

        BigDecimal remainingCoverage = policy.getCoverageLimit() != null
                ? policy.getCoverageLimit().subtract(policy.getCoveredAmount())
                : new BigDecimal("9999999.99");

        BigDecimal deductibleRemaining = policy.getDeductibleAmount() != null
                ? policy.getDeductibleAmount().subtract(policy.getDeductibleMet()).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        boolean covered = remainingCoverage.compareTo(request.getAmount()) >= 0;

        return CoverageCheckResponse.builder()
                .patientId(request.getPatientId())
                .covered(covered)
                .policyNumber(policy.getPolicyNumber())
                .policyType(policy.getPolicyType())
                .policyStatus(policy.getStatus())
                .requestedAmount(request.getAmount())
                .remainingCoverage(remainingCoverage)
                .deductibleRemaining(deductibleRemaining)
                .coverageEndDate(policy.getCoverageEndDate())
                .message(covered ? "Amount is within coverage limits" : "Requested amount exceeds remaining coverage")
                .build();
    }

    private String generatePolicyNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        long count = policyRepository.count() + 1;
        return String.format("POL-%s-%05d", year, count);
    }
}
