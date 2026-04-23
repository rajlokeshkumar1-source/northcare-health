package com.northcare.hospitalcore.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lab_results", indexes = {
        @Index(name = "idx_lab_results_patient", columnList = "patient_id"),
        @Index(name = "idx_lab_results_date", columnList = "result_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "test_name", nullable = false, length = 200)
    private String testName;

    // LOINC code (Logical Observation Identifiers Names and Codes)
    @Column(name = "test_code", length = 50)
    private String testCode;

    @Column(name = "result", nullable = false)
    private String result;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(name = "is_abnormal", nullable = false)
    private boolean isAbnormal;

    @Column(name = "ordered_by", length = 200)
    private String orderedBy;

    @Column(name = "result_date")
    private LocalDateTime resultDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
