package com.northcare.telehealth.service;

import com.northcare.telehealth.dto.MonitoringRequest;
import com.northcare.telehealth.dto.MonitoringResponse;
import com.northcare.telehealth.exception.ResourceNotFoundException;
import com.northcare.telehealth.model.RemoteMonitoring;
import com.northcare.telehealth.repository.RemoteMonitoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RemoteMonitoringService {

    private final RemoteMonitoringRepository monitoringRepository;

    @Transactional
    public MonitoringResponse ingestReading(MonitoringRequest request) {
        boolean alert = isOutsideThresholds(
                request.getValue(), request.getAlertThresholdMin(), request.getAlertThresholdMax());

        RemoteMonitoring reading = RemoteMonitoring.builder()
                .patientId(request.getPatientId())
                .deviceId(request.getDeviceId())
                .deviceType(request.getDeviceType())
                .metricName(request.getMetricName())
                .value(request.getValue())
                .unit(request.getUnit())
                .isAlert(alert)
                .alertThresholdMin(request.getAlertThresholdMin())
                .alertThresholdMax(request.getAlertThresholdMax())
                .recordedAt(request.getRecordedAt())
                .build();

        RemoteMonitoring saved = monitoringRepository.save(reading);

        if (alert) {
            log.warn("ALERT: Reading outside range – patientId={}, metric={}, value={} {}",
                    request.getPatientId(), request.getMetricName(),
                    request.getValue(), request.getUnit());
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<MonitoringResponse> getReadingsByPatient(UUID patientId, int page, int size) {
        return monitoringRepository
                .findByPatientIdOrderByRecordedAtDesc(patientId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<MonitoringResponse> getAlertsByPatient(UUID patientId) {
        return monitoringRepository.findByPatientIdAndIsAlertTrue(patientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean isOutsideThresholds(Double value, Double min, Double max) {
        if (value == null) return false;
        if (min != null && value < min) return true;
        if (max != null && value > max) return true;
        return false;
    }

    private MonitoringResponse toResponse(RemoteMonitoring m) {
        return MonitoringResponse.builder()
                .id(m.getId())
                .patientId(m.getPatientId())
                .deviceId(m.getDeviceId())
                .deviceType(m.getDeviceType())
                .metricName(m.getMetricName())
                .value(m.getValue())
                .unit(m.getUnit())
                .isAlert(m.getIsAlert())
                .alertThresholdMin(m.getAlertThresholdMin())
                .alertThresholdMax(m.getAlertThresholdMax())
                .recordedAt(m.getRecordedAt())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
