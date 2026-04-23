package com.northcare.hospitalcore.dto;

import com.northcare.hospitalcore.model.Patient.Gender;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PatientResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;

    // PHI: ssnLast4 is intentionally EXCLUDED from responses
    // It is write-only — never returned to callers

    private String bloodType;
    private List<String> allergies;
    private List<String> diagnosisCodes;
    private List<String> medications;
    private WardResponse ward;
    private LocalDateTime admissionDate;
    private LocalDateTime dischargeDate;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
