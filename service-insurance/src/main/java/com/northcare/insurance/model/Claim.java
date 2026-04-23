package com.northcare.insurance.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

    public enum ClaimStatus {
        SUBMITTED, UNDER_REVIEW, APPROVED, PARTIALLY_APPROVED, DENIED, APPEALED, PAID
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "claim_number", unique = true, nullable = false, length = 20)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "claim_date", nullable = false)
    private LocalDate claimDate;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Type(JsonBinaryType.class)
    @Column(name = "diagnosis_codes", columnDefinition = "jsonb")
    private JsonNode diagnosisCodes;

    @Type(JsonBinaryType.class)
    @Column(name = "procedure_codes", columnDefinition = "jsonb")
    private JsonNode procedureCodes;

    @Column(name = "billed_amount", precision = 12, scale = 2)
    private BigDecimal billedAmount;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    @Builder.Default
    private ClaimStatus status = ClaimStatus.SUBMITTED;

    @Column(name = "denial_reason", columnDefinition = "TEXT")
    private String denialReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
