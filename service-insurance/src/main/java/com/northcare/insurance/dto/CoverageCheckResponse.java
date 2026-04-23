package com.northcare.insurance.dto;

import com.northcare.insurance.model.InsurancePolicy.PolicyStatus;
import com.northcare.insurance.model.InsurancePolicy.PolicyType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CoverageCheckResponse {
    private UUID patientId;
    private boolean covered;
    private String policyNumber;
    private PolicyType policyType;
    private PolicyStatus policyStatus;
    private BigDecimal requestedAmount;
    private BigDecimal remainingCoverage;
    private BigDecimal deductibleRemaining;
    private String message;
    private LocalDate coverageEndDate;
}
