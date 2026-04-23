package com.northcare.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceRequest {

    @NotNull(message = "patientId is required")
    private UUID patientId;

    @NotBlank(message = "patientName is required")
    private String patientName;

    @NotNull(message = "serviceDate is required")
    private LocalDate serviceDate;

    @NotNull(message = "dueDate is required")
    private LocalDate dueDate;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<LineItemRequest> lineItems;

    private String notes;

    @Data
    public static class LineItemRequest {

        @NotBlank(message = "serviceCode is required")
        private String serviceCode;

        @NotBlank(message = "description is required")
        private String description;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        private Integer quantity;

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.01", message = "unitPrice must be positive")
        private BigDecimal unitPrice;
    }
}
