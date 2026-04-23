package com.northcare.billing.dto;

import com.northcare.billing.model.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class InvoiceResponse {

    private UUID id;
    private String invoiceNumber;
    private UUID patientId;
    private String patientName;
    private LocalDate serviceDate;
    private LocalDate dueDate;
    private List<LineItemResponse> lineItems;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private InvoiceStatus status;
    private String notes;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class LineItemResponse {
        private UUID id;
        private String serviceCode;
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}
