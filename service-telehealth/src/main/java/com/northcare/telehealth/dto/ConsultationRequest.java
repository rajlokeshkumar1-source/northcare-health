package com.northcare.telehealth.dto;

import com.northcare.telehealth.model.ConsultationType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ConsultationRequest {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Doctor ID is required")
    private UUID doctorId;

    @NotBlank(message = "Doctor name is required")
    private String doctorName;

    @NotNull(message = "Scheduled time is required")
    @Future(message = "Scheduled time must be in the future")
    private LocalDateTime scheduledAt;

    @NotNull(message = "Consultation type is required")
    private ConsultationType consultationType;

    @NotBlank(message = "Chief complaint is required")
    private String chiefComplaint;
}
