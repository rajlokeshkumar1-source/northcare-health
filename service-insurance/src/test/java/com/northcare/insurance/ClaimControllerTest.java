package com.northcare.insurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northcare.insurance.dto.ApproveClaimRequest;
import com.northcare.insurance.dto.ClaimRequest;
import com.northcare.insurance.dto.DenyClaimRequest;
import com.northcare.insurance.model.Claim;
import com.northcare.insurance.model.Claim.ClaimStatus;
import com.northcare.insurance.model.InsurancePolicy;
import com.northcare.insurance.model.InsurancePolicy.PolicyStatus;
import com.northcare.insurance.model.InsurancePolicy.PolicyType;
import com.northcare.insurance.repository.ClaimRepository;
import com.northcare.insurance.repository.InsurancePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InsurancePolicyRepository policyRepository;

    @Autowired
    private ClaimRepository claimRepository;

    private InsurancePolicy testPolicy;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        claimRepository.deleteAll();
        policyRepository.deleteAll();

        patientId = UUID.randomUUID();

        testPolicy = InsurancePolicy.builder()
                .policyNumber("POL-2025-TEST01")
                .patientId(patientId)
                .providerId(UUID.randomUUID())
                .providerName("Blue Cross Canada")
                .policyType(PolicyType.PREMIUM)
                .coverageStartDate(LocalDate.now().minusMonths(1))
                .coverageEndDate(LocalDate.now().plusYears(1))
                .deductibleAmount(new BigDecimal("500.00"))
                .coverageLimit(new BigDecimal("100000.00"))
                .status(PolicyStatus.ACTIVE)
                .isActive(true)
                .build();

        testPolicy = policyRepository.save(testPolicy);
    }

    @Test
    void submitClaim_withValidRequest_returns201() throws Exception {
        ClaimRequest request = new ClaimRequest();
        request.setPolicyId(testPolicy.getId());
        request.setPatientId(patientId);
        request.setServiceDate(LocalDate.now().minusDays(3));
        request.setBilledAmount(new BigDecimal("1500.00"));

        mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimNumber").value(org.hamcrest.Matchers.startsWith("CLM-")))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.billedAmount").value(1500.00));
    }

    @Test
    void approveClaim_afterReview_returnsApproved() throws Exception {
        // Submit
        ClaimRequest submitReq = new ClaimRequest();
        submitReq.setPolicyId(testPolicy.getId());
        submitReq.setPatientId(patientId);
        submitReq.setServiceDate(LocalDate.now().minusDays(2));
        submitReq.setBilledAmount(new BigDecimal("2000.00"));

        MvcResult submitResult = mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Claim submitted = objectMapper.readValue(submitResult.getResponse().getContentAsString(), Claim.class);

        // Review
        mockMvc.perform(put("/api/v1/claims/" + submitted.getId() + "/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        // Approve
        ApproveClaimRequest approveReq = new ApproveClaimRequest();
        approveReq.setApprovedAmount(new BigDecimal("2000.00"));

        mockMvc.perform(put("/api/v1/claims/" + submitted.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedAmount").value(2000.00));
    }

    @Test
    void denyClaim_afterReview_returnsDenied() throws Exception {
        ClaimRequest submitReq = new ClaimRequest();
        submitReq.setPolicyId(testPolicy.getId());
        submitReq.setPatientId(patientId);
        submitReq.setServiceDate(LocalDate.now().minusDays(5));
        submitReq.setBilledAmount(new BigDecimal("500.00"));

        MvcResult submitResult = mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Claim submitted = objectMapper.readValue(submitResult.getResponse().getContentAsString(), Claim.class);

        mockMvc.perform(put("/api/v1/claims/" + submitted.getId() + "/review"))
                .andExpect(status().isOk());

        DenyClaimRequest denyReq = new DenyClaimRequest();
        denyReq.setDenialReason("Procedure not covered under current policy");

        mockMvc.perform(put("/api/v1/claims/" + submitted.getId() + "/deny")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(denyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DENIED"))
                .andExpect(jsonPath("$.denialReason").value("Procedure not covered under current policy"));
    }

    @Test
    void getClaimsByPatient_returnsPagedResults() throws Exception {
        Claim claim = Claim.builder()
                .claimNumber("CLM-2025-T0001")
                .policy(testPolicy)
                .patientId(patientId)
                .claimDate(LocalDate.now())
                .serviceDate(LocalDate.now().minusDays(1))
                .billedAmount(new BigDecimal("750.00"))
                .status(ClaimStatus.SUBMITTED)
                .build();
        claimRepository.save(claim);

        mockMvc.perform(get("/api/v1/claims/patient/" + patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].patientId").value(patientId.toString()));
    }

    @Test
    void submitClaim_withInactivePolicyId_returns422() throws Exception {
        testPolicy.setStatus(PolicyStatus.SUSPENDED);
        policyRepository.save(testPolicy);

        ClaimRequest request = new ClaimRequest();
        request.setPolicyId(testPolicy.getId());
        request.setPatientId(patientId);
        request.setServiceDate(LocalDate.now());
        request.setBilledAmount(new BigDecimal("300.00"));

        mockMvc.perform(post("/api/v1/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}
