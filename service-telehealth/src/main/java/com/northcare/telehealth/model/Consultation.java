package com.northcare.telehealth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consultations", indexes = {
        @Index(name = "idx_consultation_patient_id", columnList = "patient_id"),
        @Index(name = "idx_consultation_status", columnList = "status"),
        @Index(name = "idx_consultation_scheduled_at", columnList = "scheduled_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "doctor_name", nullable = false)
    private String doctorName;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "actual_start_at")
    private LocalDateTime actualStartAt;

    @Column(name = "actual_end_at")
    private LocalDateTime actualEndAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConsultationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type", nullable = false, length = 10)
    private ConsultationType consultationType;

    @Column(name = "chief_complaint", length = 1000)
    private String chiefComplaint;

    /** HIPAA PHI – Doctor notes. Restrict to authorised single-record responses only. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "meeting_url", length = 500)
    private String meetingUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    // Using Boolean (boxed) to keep Lombok getter/setter naming unambiguous
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
