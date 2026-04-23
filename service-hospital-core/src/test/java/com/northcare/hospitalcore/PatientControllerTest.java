package com.northcare.hospitalcore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northcare.hospitalcore.dto.PatientRequest;
import com.northcare.hospitalcore.model.Patient.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class PatientControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("northcare_test")
            .withUsername("northcare_test")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("northcare.aws.db-secret-name", () -> "");
        registry.add("ENCRYPTION_KEY", () -> "test-encryption-key-32-chars-long!!");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private PatientRequest buildValidRequest() {
        PatientRequest req = new PatientRequest();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setDateOfBirth(LocalDate.of(1985, 6, 15));
        req.setGender(Gender.FEMALE);
        req.setSsnLast4("1234");
        req.setBloodType("A+");
        req.setAllergies(List.of("Penicillin", "Latex"));
        req.setDiagnosisCodes(List.of("J18.9", "E11.9"));
        req.setMedications(List.of("Metformin 500mg", "Lisinopril 10mg"));
        return req;
    }

    @Test
    void testCreatePatient_success() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                // PHI: ssnLast4 must never appear in response
                .andExpect(jsonPath("$.ssnLast4").doesNotExist());
    }

    @Test
    void testGetPatient_notFound() throws Exception {
        UUID nonExistent = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/patients/{id}", nonExistent))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void testGetPatients_paginated() throws Exception {
        // Create a patient first
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/patients")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void testDeletePatient_softDelete() throws Exception {
        // Create patient
        MvcResult createResult = mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String patientId = objectMapper.readTree(responseBody).get("id").asText();

        // Delete (soft)
        mockMvc.perform(delete("/api/v1/patients/{id}", patientId))
                .andExpect(status().isNoContent());

        // Subsequent GET must return 404 (soft-deleted record not returned)
        mockMvc.perform(get("/api/v1/patients/{id}", patientId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreatePatient_invalidInput() throws Exception {
        PatientRequest invalid = new PatientRequest();
        // firstName, lastName, dateOfBirth, gender are all missing

        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }
}
