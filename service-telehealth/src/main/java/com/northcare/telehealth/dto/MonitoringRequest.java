package com.northcare.telehealth.dto;

import com.northcare.telehealth.model.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MonitoringRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotNull(message = "Device type is required")
    private DeviceType deviceType;

    @NotBlank(message = "Metric name is required")
    private String metricName;

    @NotNull(message = "Value is required")
    private Double value;

    @NotBlank(message = "Unit is required")
    private String unit;

    private Double alertThresholdMin;
    private Double alertThresholdMax;

    @NotNull(message = "Recorded time is required")
    private LocalDateTime recordedAt;
}
