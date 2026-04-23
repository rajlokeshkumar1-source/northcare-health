package com.northcare.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northcare.notifications.dto.*;
import com.northcare.notifications.model.*;
import com.northcare.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    // ─────────────────────────────────────────────
    //  POST /api/v1/notifications  → 201
    // ─────────────────────────────────────────────

    @Test
    void testSendNotification_success() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
                .recipientId(UUID.randomUUID())
                .recipientType(RecipientType.PATIENT)
                .recipientEmail("patient@northcare.test")
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.GENERAL)
                .subject("Welcome to NorthCare")
                .message("Your account has been created successfully.")
                .priority(NotificationPriority.NORMAL)
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.recipientEmail").value("patient@northcare.test"))
                .andExpect(jsonPath("$.status").value(anyOf(is("SENT"), is("DELIVERED"), is("FAILED"))));
    }

    // ─────────────────────────────────────────────
    //  POST /api/v1/notifications/emergency  → 200
    // ─────────────────────────────────────────────

    @Test
    void testEmergencyBroadcast() throws Exception {
        EmergencyBroadcastRequest request = EmergencyBroadcastRequest.builder()
                .message("EMERGENCY: Hospital evacuation required")
                .recipientIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .channels(List.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP))
                .build();

        // 2 recipients × 2 channels = 4 notifications
        mockMvc.perform(post("/api/v1/notifications/emergency")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4))
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.notifications", hasSize(4)));
    }

    // ─────────────────────────────────────────────
    //  POST /api/v1/notifications/from-template → 201
    // ─────────────────────────────────────────────

    @Test
    void testSendFromTemplate_success() throws Exception {
        TemplateNotificationRequest request = TemplateNotificationRequest.builder()
                .templateCode("APPOINTMENT_REMINDER_24H")
                .recipientId(UUID.randomUUID())
                .recipientType(RecipientType.PATIENT)
                .recipientEmail("patient@northcare.test")
                .variables(Map.of(
                        "patientName",     "Jane Doe",
                        "appointmentDate", "2025-01-15",
                        "appointmentTime", "10:30 AM",
                        "doctorName",      "Dr. Smith",
                        "location",        "Room 201, Building A"
                ))
                .build();

        mockMvc.perform(post("/api/v1/notifications/from-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.type").value("APPOINTMENT_REMINDER"));
    }

    // ─────────────────────────────────────────────
    //  PUT /api/v1/notifications/{id}/read  → 200
    // ─────────────────────────────────────────────

    @Test
    void testMarkAsRead() throws Exception {
        // Create an IN_APP notification (status = DELIVERED, can be marked READ)
        NotificationRequest createRequest = NotificationRequest.builder()
                .recipientId(UUID.randomUUID())
                .recipientType(RecipientType.DOCTOR)
                .recipientEmail("doctor@northcare.test")
                .channel(NotificationChannel.IN_APP)
                .type(NotificationType.LAB_RESULT_READY)
                .subject("Lab result ready")
                .message("Patient CBC results are available.")
                .priority(NotificationPriority.HIGH)
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(put("/api/v1/notifications/{id}/read", id))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────
    //  GET /api/v1/notifications/recipient/{id}  → paginated 200
    // ─────────────────────────────────────────────

    @Test
    void testGetByRecipient_paginated() throws Exception {
        UUID recipientId = UUID.randomUUID();

        // Create 3 notifications for the same recipient
        for (int i = 0; i < 3; i++) {
            NotificationRequest req = NotificationRequest.builder()
                    .recipientId(recipientId)
                    .recipientType(RecipientType.PATIENT)
                    .recipientEmail("patient" + i + "@northcare.test")
                    .channel(NotificationChannel.EMAIL)
                    .type(NotificationType.GENERAL)
                    .subject("Notification " + i)
                    .message("Message body " + i)
                    .priority(NotificationPriority.LOW)
                    .build();

            mockMvc.perform(post("/api/v1/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/notifications/recipient/{recipientId}", recipientId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].recipientId").value(recipientId.toString()));
    }
}
