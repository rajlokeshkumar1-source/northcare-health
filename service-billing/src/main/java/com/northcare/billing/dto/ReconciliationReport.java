package com.northcare.billing.dto;

import com.northcare.billing.model.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class ReconciliationReport {

    private LocalDate date;
    private int totalTransactions;
    private BigDecimal totalAmount;
    private Map<PaymentMethod, BigDecimal> amountByMethod;
    private Map<PaymentMethod, Long> countByMethod;
}
