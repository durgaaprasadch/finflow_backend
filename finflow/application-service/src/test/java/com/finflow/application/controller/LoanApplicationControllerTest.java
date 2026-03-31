package com.finflow.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.LoanStatus;
import com.finflow.application.exception.ApplicationException;
import com.finflow.application.service.LoanApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WebMvcTest(LoanApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class LoanApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanApplicationService service;

    @Autowired
    private ObjectMapper objectMapper;

    private LoanApplication app;

    @BeforeEach
    void setUp() {
        app = new LoanApplication();
        app.setId(1L);
        app.setApplicantUsername("testuser");
        app.setAmount(java.math.BigDecimal.valueOf(5000.0));
        app.setStatus(LoanStatus.DRAFT);
    }

    @Test
    void createDraft_ShouldInjectHeaderUsernameAndReturnApp() throws Exception {
        when(service.createDraft(any(LoanApplication.class))).thenReturn(app);

        com.finflow.application.dto.CreateApplicationRequest req = new com.finflow.application.dto.CreateApplicationRequest();
        req.setLoanType(com.finflow.application.entity.LoanType.PERSONAL_LOAN);
        req.setRequestedAmount(java.math.BigDecimal.valueOf(5000.0));

        mockMvc.perform(post("/api/applications")
                .header("loggedInUser", "testuser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.applicantUsername").value("testuser"));
    }

    @Test
    void getMyApplications_ShouldReturnList() throws Exception {
        LoanApplication mockApp = new LoanApplication();
        mockApp.setId(1L);
        mockApp.setApplicantUsername("testuser");
        mockApp.setStatus(LoanStatus.DRAFT);
        when(service.getApplicationsByUser("testuser")).thenReturn(java.util.Collections.singletonList(mockApp));

        mockMvc.perform(get("/api/applications/me")
                .header("loggedInUser", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].applicantUsername").value("testuser"));
    }

    @Test
    void createDraft_ShouldReturnBadRequest_WhenHeaderIsMissing() throws Exception {
        com.finflow.application.dto.CreateApplicationRequest req = new com.finflow.application.dto.CreateApplicationRequest();
        
        mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()); // Missing RequestHeader automatically yields 400
    }

    @Test
    void getApplication_ShouldReturnApplication() throws Exception {
        when(service.getApplicationById(1L)).thenReturn(app);

        mockMvc.perform(get("/api/applications/1")
                .header("userRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateStatus_ShouldReturnUpdatedApplication() throws Exception {
        LoanApplication mockApp = new LoanApplication();
        mockApp.setId(1L);
        mockApp.setApplicantUsername("testuser");
        mockApp.setStatus(LoanStatus.SUBMITTED);
        when(service.getApplicationById(1L)).thenReturn(mockApp);
        when(service.updateStatus(eq(1L), eq(LoanStatus.SUBMITTED), anyString(), anyString())).thenReturn(mockApp);

        mockMvc.perform(patch("/api/applications/1/status")
                .header("loggedInUser", "testuser")
                .header("userRole", "APPLICANT")
                .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void getApplication_ShouldThrowException_WhenApplicantFetchesOthersApp() throws Exception {
        LoanApplication otherApp = new LoanApplication();
        otherApp.setId(2L);
        otherApp.setApplicantUsername("different_user");

        when(service.getApplicationById(2L)).thenReturn(otherApp);

        mockMvc.perform(get("/api/applications/2")
                .header("userRole", "APPLICANT")
                .header("loggedInUser", "testuser"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof RuntimeException))
                .andExpect(result -> assertTrue(java.util.Objects.requireNonNull(result.getResolvedException()).getMessage().contains("Unauthorized")));
    }

    @Test
    void getAllApplications_ShouldThrowException_WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/applications/all")
                .header("userRole", "APPLICANT"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ApplicationException))
                .andExpect(result -> assertTrue(java.util.Objects.requireNonNull(result.getResolvedException()).getMessage().contains("Unauthorized: Only ADMIN can view all applications")));
    }

    @Test
    void getAllApplications_ShouldReturnList_WhenAdmin() throws Exception {
        when(service.getAllApplications()).thenReturn(java.util.Collections.singletonList(app));

        mockMvc.perform(get("/api/applications/all")
                .header("userRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminUpdateStatus_ShouldFormatHiddenEndpointCorrectly() throws Exception {
        when(service.updateStatus(1L, LoanStatus.REJECTED, "ADMIN", null)).thenReturn(app);

        mockMvc.perform(patch("/api/applications/1/status")
                .header("userRole", "ADMIN")
                .param("status", "REJECTED"))
                .andExpect(status().isOk());
    }

    @Test
    void adminUpdateStatus_ShouldBlockApplicantFromArbitraryStatus() throws Exception {
        when(service.updateStatus(1L, LoanStatus.APPROVED, "APPLICANT", null))
            .thenThrow(new ApplicationException("Only ADMIN can set status to APPROVED"));

        mockMvc.perform(patch("/api/applications/1/status")
                .header("userRole", "APPLICANT")
                .param("status", "APPROVED"))
                .andExpect(result -> assertTrue(java.util.Objects.requireNonNull(result.getResolvedException()).getMessage().contains("Only ADMIN can set status to APPROVED")));
    }
}
