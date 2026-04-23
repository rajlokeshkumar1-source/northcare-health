package com.northcare.telehealth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.northcare.telehealth.model.DeviceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitoringResponse {

    private UUID id;
    private UUID patientId;
    private String deviceId;
    private DeviceType deviceType;
    private String metricName;
    private Double value;
    private String unit;
    private Boolean isAlert;
    private Double alertThresholdMin;
    private Double alertThresholdMax;
    private LocalDateTime recordedAt;
    private LocalDateTime createdAt;
}
