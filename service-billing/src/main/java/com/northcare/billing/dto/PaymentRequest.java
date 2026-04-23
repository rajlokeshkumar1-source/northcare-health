package com.northcare.billing.dto;

import com.northcare.billing.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentRequest {

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "paymentDate is required")
    private LocalDate paymentDate;

    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    private String referenceNumber;
}
