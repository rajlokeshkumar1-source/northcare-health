package com.northcare.insurance.dto;

import com.northcare.insurance.model.InsurancePolicy.PolicyType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PolicyRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Provider ID is required")
    private UUID providerId;

    @NotBlank(message = "Provider name is required")
    @Size(max = 150)
    private String providerName;

    @NotNull(message = "Policy type is required")
    private PolicyType policyType;

    @NotNull(message = "Coverage start date is required")
    private LocalDate coverageStartDate;

    @NotNull(message = "Coverage end date is required")
    private LocalDate coverageEndDate;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal deductibleAmount;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal coverageLimit;

    @Size(max = 50)
    private String groupNumber;

    @Size(max = 50)
    private String memberNumber;
}
