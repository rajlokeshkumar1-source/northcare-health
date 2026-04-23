package com.northcare.insurance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApproveClaimRequest {

    @NotNull(message = "Approved amount is required")
    @DecimalMin(value = "0.01", message = "Approved amount must be greater than 0")
    private BigDecimal approvedAmount;

    private String notes;
}
