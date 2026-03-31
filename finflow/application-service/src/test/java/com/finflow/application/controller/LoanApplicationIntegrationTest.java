package com.finflow.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.LoanStatus;
import com.finflow.application.repository.LoanApplicationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.rabbitmq.test.context=true"
})
@AutoConfigureMockMvc
@SuppressWarnings("null")
class LoanApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanApplicationRepository repository;

    @Autowired
    private com.finflow.application.repository.LoanStatusHistoryRepository historyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        historyRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    void testFullApplicationLifecycle() throws Exception {
        // 1. Create a draft application
        LoanApplication app = new LoanApplication();
        app.setAmount(java.math.BigDecimal.valueOf(15000));

        String postResponse = mockMvc.perform(post("/api/applications")
                .header("loggedInUser", "integration_user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(app)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.applicantUsername").value("integration_user"))
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(postResponse);
        Long appId = root.get("id").asLong();

        // 2. Fetch it by ID (Must be Admin to fetch directly)
        mockMvc.perform(get("/api/applications/" + appId)
                .header("userRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(15000.0));

        // 3. Fetch applications by User (using /me endpoint)
        mockMvc.perform(get("/api/applications/me")
                .header("loggedInUser", "integration_user")
                .header("userRole", "APPLICANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].applicantUsername").value("integration_user"));

        // 4. Submit the Draft
        mockMvc.perform(patch("/api/applications/" + appId + "/status")
                .header("loggedInUser", "integration_user")
                .header("userRole", "APPLICANT")
                .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        // 5. Verify the DRAFT -> SUBMITTED transition actually persisted
        LoanApplication updatedApp = repository.findById(appId).get();
        assertEquals(LoanStatus.SUBMITTED, updatedApp.getStatus());
        
        // 6. Test Invalid Transition (SUBMITTED -> APPROVED directly violates state machine)
        mockMvc.perform(patch("/api/applications/" + appId + "/status")
                .header("loggedInUser", "integration_user")
                .header("userRole", "ADMIN")
                .param("status", "APPROVED"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof com.finflow.application.exception.InvalidTransitionException)); 

        // 7. Exhaustive Valid Transitions down to CLOSED
        mockMvc.perform(patch("/api/applications/" + appId + "/status")
                .header("userRole", "ADMIN")
                .param("status", "DOCS_PENDING")).andExpect(status().isOk());
                
        mockMvc.perform(patch("/api/applications/" + appId + "/status")
                .header("userRole", "ADMIN")
                .param("status", "UPLOADED")).andExpect(status().isOk());
                
        mockMvc.perform(patch("/api/applications/" + appId + "/status")
                .header("userRole", "ADMIN")
                .param("status", "REVIEW")).andExpect(status().isOk());
                
        mockMvc.perform(patch("/api/applications/" + appId + "/status")
                .header("userRole", "ADMIN")
                .param("status", "VERIFIED")).andExpect(status().isOk());
                
        mockMvc.perform(patch("/api/applications/" + appId + "/status")
                .header("userRole", "ADMIN")
                .param("status", "APPROVED")).andExpect(status().isOk());
                
        mockMvc.perform(patch("/api/applications/" + appId + "/status")
                .header("userRole", "ADMIN")
                .param("status", "CLOSED")).andExpect(status().isOk());

        // 8. Verify CLOSED state lock
        mockMvc.perform(patch("/api/applications/" + appId + "/status")
                .header("userRole", "ADMIN")
                .param("status", "REVIEW"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof com.finflow.application.exception.InvalidTransitionException)); 
    }
}
