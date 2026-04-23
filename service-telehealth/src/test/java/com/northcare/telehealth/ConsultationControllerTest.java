package com.northcare.telehealth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northcare.telehealth.controller.ConsultationController;
import com.northcare.telehealth.dto.ConsultationRequest;
import com.northcare.telehealth.dto.ConsultationResponse;
import com.northcare.telehealth.exception.GlobalExceptionHandler;
import com.northcare.telehealth.exception.ResourceNotFoundException;
import com.northcare.telehealth.model.ConsultationStatus;
import com.northcare.telehealth.model.ConsultationType;
import com.northcare.telehealth.service.ConsultationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConsultationController.class)
@Import(GlobalExceptionHandler.class)
class ConsultationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsultationService consultationService;

    private UUID patientId;
    private UUID doctorId;
    private UUID consultationId;
    private ConsultationResponse sampleResponse;

    @BeforeEach
    void setUp() {
        patientId      = UUID.randomUUID();
        doctorId       = UUID.randomUUID();
        consultationId = UUID.randomUUID();

        sampleResponse = ConsultationResponse.builder()
                .id(consultationId)
                .patientId(patientId)
                .doctorId(doctorId)
                .doctorName("Dr. Smith")
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .status(ConsultationStatus.SCHEDULED)
                .consultationType(ConsultationType.VIDEO)
                .chiefComplaint("Headache")
                .meetingUrl("https://meet.northcare.health/" + UUID.randomUUID())
                .isActive(Boolean.TRUE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/consultations – valid request returns 201 with meeting URL")
    void scheduleConsultation_validRequest_returns201() throws Exception {
        ConsultationRequest request = new ConsultationRequest();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);
        request.setDoctorName("Dr. Smith");
        request.setScheduledAt(LocalDateTime.now().plusDays(1));
        request.setConsultationType(ConsultationType.VIDEO);
        request.setChiefComplaint("Headache");

        when(consultationService.scheduleConsultation(any(ConsultationRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(consultationId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.meetingUrl").exists());

        verify(consultationService).scheduleConsultation(any(ConsultationRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/consultations – missing fields returns 400 with validation details")
    void scheduleConsultation_missingFields_returns400() throws Exception {
        // Empty request body – all required fields missing
        ConsultationRequest invalid = new ConsultationRequest();

        mockMvc.perform(post("/api/v1/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());

        verifyNoInteractions(consultationService);
    }

    @Test
    @DisplayName("GET /api/v1/consultations/patient/{id} – returns paged results, no PHI notes")
    void getByPatient_returnsPagedResults() throws Exception {
        PageImpl<ConsultationResponse> page =
                new PageImpl<>(List.of(sampleResponse), PageRequest.of(0, 20), 1);

        when(consultationService.getConsultationsByPatient(eq(patientId), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/consultations/patient/{patientId}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(consultationId.toString()))
                .andExpect(jsonPath("$.content[0].patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                // PHI guard: notes must be absent from list response
                .andExpect(jsonPath("$.content[0].notes").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /api/v1/consultations/{id}/cancel – cancels and returns CANCELLED status")
    void cancelConsultation_returns200WithCancelledStatus() throws Exception {
        ConsultationResponse cancelled = ConsultationResponse.builder()
                .id(consultationId)
                .patientId(patientId)
                .status(ConsultationStatus.CANCELLED)
                .isActive(Boolean.FALSE)
                .build();

        when(consultationService.cancelConsultation(eq(consultationId), anyString()))
                .thenReturn(cancelled);

        mockMvc.perform(patch("/api/v1/consultations/{id}/cancel", consultationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Patient request\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/consultations/{id} – unknown ID returns 404")
    void getById_unknownId_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(consultationService.getConsultationById(unknownId))
                .thenThrow(new ResourceNotFoundException("Consultation not found with id: " + unknownId));

        mockMvc.perform(get("/api/v1/consultations/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("PATCH /api/v1/consultations/{id}/start – starts consultation in SCHEDULED state")
    void startConsultation_returns200WithInProgressStatus() throws Exception {
        ConsultationResponse inProgress = ConsultationResponse.builder()
                .id(consultationId)
                .patientId(patientId)
                .status(ConsultationStatus.IN_PROGRESS)
                .actualStartAt(LocalDateTime.now())
                .isActive(Boolean.TRUE)
                .build();

        when(consultationService.startConsultation(consultationId)).thenReturn(inProgress);

        mockMvc.perform(patch("/api/v1/consultations/{id}/start", consultationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.actualStartAt").exists());
    }
}
