package com.northcare.insurance.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ClaimRequest {

    @NotNull(message = "Policy ID is required")
    private UUID policyId;

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    private UUID invoiceId;

    @NotNull(message = "Service date is required")
    private LocalDate serviceDate;

    private JsonNode diagnosisCodes;

    private JsonNode procedureCodes;

    @NotNull(message = "Billed amount is required")
    @DecimalMin(value = "0.01", message = "Billed amount must be greater than 0")
    private BigDecimal billedAmount;

    private String notes;
}
