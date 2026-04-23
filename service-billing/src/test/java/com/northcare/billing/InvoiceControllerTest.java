package com.northcare.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.northcare.billing.dto.InvoiceRequest;
import com.northcare.billing.dto.PaymentRequest;
import com.northcare.billing.model.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvoiceControllerTest {

    private static final String BASE = "/api/v1/invoices";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private InvoiceRequest buildRequest() {
        InvoiceRequest.LineItemRequest li = new InvoiceRequest.LineItemRequest();
        li.setServiceCode("99213");
        li.setDescription("Office Visit – Level 3");
        li.setQuantity(1);
        li.setUnitPrice(new BigDecimal("150.00"));

        InvoiceRequest req = new InvoiceRequest();
        req.setPatientId(UUID.randomUUID());
        req.setPatientName("Jane Smith");
        req.setServiceDate(LocalDate.now());
        req.setDueDate(LocalDate.now().plusDays(30));
        req.setLineItems(List.of(li));
        return req;
    }

    private String createInvoice() throws Exception {
        MvcResult result = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /invoices → 201, DRAFT, invoice number generated, currency CAD")
    void createInvoice_success() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.lineItems").isArray())
                .andExpect(jsonPath("$.totalAmount").isNotEmpty());
    }

    @Test
    @DisplayName("POST /invoices with missing fields → 400 with violation details")
    void createInvoice_validationFailure() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations").isMap());
    }

    @Test
    @DisplayName("GET /invoices/{id} → 200")
    void getInvoice_found() throws Exception {
        String id = createInvoice();
        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("GET /invoices/{id} with unknown id → 404")
    void getInvoice_notFound() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /invoices/{id}/issue → status changes DRAFT → ISSUED")
    void issueInvoice_draftToIssued() throws Exception {
        String id = createInvoice();
        mockMvc.perform(put(BASE + "/{id}/issue", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    @DisplayName("PUT /invoices/{id}/issue twice → 409 conflict")
    void issueInvoice_alreadyIssued_conflict() throws Exception {
        String id = createInvoice();
        mockMvc.perform(put(BASE + "/{id}/issue", id));
        mockMvc.perform(put(BASE + "/{id}/issue", id))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /invoices/{id}/payments with full amount → status PAID")
    void processPayment_fullPayment_paid() throws Exception {
        String id = createInvoice();
        mockMvc.perform(put(BASE + "/{id}/issue", id));

        // Read total amount
        MvcResult getResult = mockMvc.perform(get(BASE + "/{id}", id)).andReturn();
        JsonNode node = objectMapper.readTree(getResult.getResponse().getContentAsString());
        BigDecimal total = new BigDecimal(node.get("totalAmount").asText());

        PaymentRequest payment = new PaymentRequest();
        payment.setAmount(total);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setReferenceNumber("REF-TEST-001");

        mockMvc.perform(post(BASE + "/{id}/payments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("POST /invoices/{id}/payments with partial amount → status PARTIALLY_PAID")
    void processPayment_partialPayment_partiallyPaid() throws Exception {
        String id = createInvoice();
        mockMvc.perform(put(BASE + "/{id}/issue", id));

        PaymentRequest payment = new PaymentRequest();
        payment.setAmount(new BigDecimal("10.00"));
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentMethod(PaymentMethod.CASH);

        mockMvc.perform(post(BASE + "/{id}/payments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));
    }

    @Test
    @DisplayName("GET /invoices/overdue → returns list (may be empty in test)")
    void getOverdueInvoices_returnsList() throws Exception {
        mockMvc.perform(get(BASE + "/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /invoices/reconciliation?date=today → returns report")
    void reconciliation_returnsReport() throws Exception {
        MvcResult result = mockMvc.perform(
                        get(BASE + "/reconciliation").param("date", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode report = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(report.has("date")).isTrue();
        assertThat(report.has("totalTransactions")).isTrue();
        assertThat(report.has("totalAmount")).isTrue();
    }

    @Test
    @DisplayName("GET /invoices → paginated list")
    void listInvoices_paginated() throws Exception {
        createInvoice();
        mockMvc.perform(get(BASE).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
