package com.northcare.telehealth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.northcare.telehealth.model.ConsultationStatus;
import com.northcare.telehealth.model.ConsultationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultationResponse {

    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private String doctorName;
    private LocalDateTime scheduledAt;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    private ConsultationStatus status;
    private ConsultationType consultationType;
    private String chiefComplaint;

    /**
     * HIPAA PHI – Doctor notes.
     * Populated only on single-record (GET /{id}) responses.
     * Excluded (null, omitted via @JsonInclude) from list/page responses.
     */
    private String notes;

    private String meetingUrl;
    private Integer durationMinutes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
