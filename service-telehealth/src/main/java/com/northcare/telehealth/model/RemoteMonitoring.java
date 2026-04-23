package com.northcare.telehealth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "remote_monitoring_readings", indexes = {
        @Index(name = "idx_monitoring_patient_id", columnList = "patient_id"),
        @Index(name = "idx_monitoring_recorded_at", columnList = "recorded_at"),
        @Index(name = "idx_monitoring_patient_alert", columnList = "patient_id, is_alert")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class RemoteMonitoring {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 30)
    private DeviceType deviceType;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    /** True when the reading falls outside the configured alert thresholds. */
    @Column(name = "is_alert", nullable = false)
    @Builder.Default
    private Boolean isAlert = Boolean.FALSE;

    @Column(name = "alert_threshold_min")
    private Double alertThresholdMin;

    @Column(name = "alert_threshold_max")
    private Double alertThresholdMax;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
