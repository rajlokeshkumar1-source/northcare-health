package com.northcare.insurance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppealClaimRequest {

    @NotBlank(message = "Appeal reason is required")
    private String reason;
}
