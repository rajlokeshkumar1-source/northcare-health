package com.northcare.hospitalcore.dto;

import com.northcare.hospitalcore.model.Patient.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PatientRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    // PHI — write-only: stored encrypted, never returned in responses
    @Size(min = 4, max = 4, message = "SSN last 4 must be exactly 4 digits")
    @Pattern(regexp = "[0-9]{4}", message = "SSN last 4 must contain only digits")
    private String ssnLast4;

    @Size(max = 5)
    private String bloodType;

    private List<String> allergies;

    private List<@NotBlank String> diagnosisCodes;

    private List<@NotBlank String> medications;

    private UUID wardId;
}
