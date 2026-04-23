package com.northcare.insurance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "insurance_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InsurancePolicy {

    public enum PolicyType {
        BASIC, EXTENDED, PREMIUM, GOVERNMENT
    }

    public enum PolicyStatus {
        ACTIVE, EXPIRED, SUSPENDED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "policy_number", unique = true, nullable = false, length = 20)
    private String policyNumber;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "provider_name", nullable = false, length = 150)
    private String providerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 20)
    private PolicyType policyType;

    @Column(name = "coverage_start_date", nullable = false)
    private LocalDate coverageStartDate;

    @Column(name = "coverage_end_date", nullable = false)
    private LocalDate coverageEndDate;

    @Column(name = "deductible_amount", precision = 12, scale = 2)
    private BigDecimal deductibleAmount;

    @Column(name = "deductible_met", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal deductibleMet = BigDecimal.ZERO;

    @Column(name = "coverage_limit", precision = 12, scale = 2)
    private BigDecimal coverageLimit;

    @Column(name = "covered_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal coveredAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PolicyStatus status = PolicyStatus.ACTIVE;

    @Column(name = "group_number", length = 50)
    private String groupNumber;

    @Column(name = "member_number", length = 50)
    private String memberNumber;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
