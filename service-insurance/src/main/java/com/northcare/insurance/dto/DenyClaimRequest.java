package com.northcare.insurance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DenyClaimRequest {

    @NotBlank(message = "Denial reason is required")
    private String denialReason;

    private String notes;
}
