package com.northcare.hospitalcore.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_patients_active", columnList = "is_active"),
        @Index(name = "idx_patients_ward", columnList = "ward_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    // PHI: AES-256 encrypted before persistence — see EncryptionUtil
    @Column(name = "ssn_last4_encrypted", length = 255)
    private String ssnLast4;

    @Column(name = "blood_type", length = 5)
    private String bloodType;

    @Type(JsonBinaryType.class)
    @Column(name = "allergies", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> allergies = new ArrayList<>();

    // ICD-10 diagnosis codes stored as JSONB
    @Type(JsonBinaryType.class)
    @Column(name = "diagnosis_codes", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> diagnosisCodes = new ArrayList<>();

    @Type(JsonBinaryType.class)
    @Column(name = "medications", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> medications = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id")
    private Ward ward;

    @Column(name = "admission_date")
    private LocalDateTime admissionDate;

    @Column(name = "discharge_date")
    private LocalDateTime dischargeDate;

    // Soft-delete flag — records are never physically deleted
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
