package com.finflow.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.application.dto.CreateApplicationRequest;
import com.finflow.application.dto.EmploymentDetailsRequest;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.LoanStatus;
import com.finflow.application.service.LoanApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicantApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class ApplicantApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanApplicationService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createApplication_ShouldReturnApplicantContract() throws Exception {
        LoanApplication app = new LoanApplication();
        app.setId(1L);
        app.setApplicantId("applicant-1");
        app.setApplicantUsername("durga@gmail.com");
        app.setLoanType("HOME_LOAN");
        app.setAmount(BigDecimal.valueOf(500000));
        app.setStatus(LoanStatus.DRAFT);
        when(service.createApplicantDraft(any(CreateApplicationRequest.class), eq("applicant-1"), eq("durga@gmail.com"))).thenReturn(app);

        mockMvc.perform(post("/api/v1/applications")
                        .header("loggedInUser", "durga@gmail.com")
                        .header("applicantId", "applicant-1")
                        .header("userRole", "APPLICANT")
                        .param("loanType", "HOME_LOAN")
                        .param("requestedAmount", "500000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Application created"))
                .andExpect(jsonPath("$.data.applicationId").value(1))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void updateEmployment_ShouldReturnApplicantIdAndStatus() throws Exception {
        LoanApplication app = new LoanApplication();
        app.setId(1L);
        app.setApplicantId("applicant-1");
        app.setStatus(LoanStatus.EMPLOYMENT_ADDED);
        app.setEmploymentType("SALARIED");
        app.setCompanyName("TCS");
        app.setDesignation("Software Engineer");
        app.setMonthlyIncome(BigDecimal.valueOf(500000)); // Adjusted to match scale
        app.setUpdatedAt(LocalDateTime.now());
        
        // Match the NO-ID signature in service
        when(service.updateEmploymentDetails(eq("applicant-1"), eq("durga@gmail.com"), any(EmploymentDetailsRequest.class))).thenReturn(app);

        EmploymentDetailsRequest request = new EmploymentDetailsRequest();
        request.setEmploymentType("SALARIED");
        request.setCompanyName("TCS");
        request.setDesignation("Software Engineer");
        request.setMonthlyIncome(BigDecimal.valueOf(500000));

        mockMvc.perform(patch("/api/v1/applications/employment")
                        .header("loggedInUser", "durga@gmail.com")
                        .header("applicantId", "applicant-1")
                        .header("userRole", "APPLICANT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(1))
                .andExpect(jsonPath("$.data.status").value("EMPLOYMENT_ADDED"));
    }

    @Test
    void getMyApplications_ShouldReturnWrappedList() throws Exception {
        LoanApplication app = new LoanApplication();
        app.setId(1L);
        app.setApplicantId("applicant-1");
        app.setLoanType("HOME_LOAN");
        app.setAmount(BigDecimal.valueOf(500000));
        app.setStatus(LoanStatus.SUBMITTED);
        when(service.getApplicationsByApplicant("applicant-1", "durga@gmail.com")).thenReturn(java.util.Collections.singletonList(app));

        mockMvc.perform(get("/api/v1/applications/history")
                        .header("loggedInUser", "durga@gmail.com")
                        .header("applicantId", "applicant-1")
                        .header("userRole", "APPLICANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].applicationId").value(1));
    }

    @Test
    void submitApplication_ShouldReturnSubmittedStatus() throws Exception {
        LoanApplication app = new LoanApplication();
        app.setId(1L);
        app.setApplicantId("applicant-1");
        app.setStatus(LoanStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());
        when(service.submitApplicantApplication("applicant-1", "durga@gmail.com")).thenReturn(app);

        mockMvc.perform(patch("/api/v1/applications/submit")
                        .header("loggedInUser", "durga@gmail.com")
                        .header("applicantId", "applicant-1")
                        .header("userRole", "APPLICANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.submittedAt").exists());
    }

    @Test
    void getApplicationStatus_ShouldReturnTimeline() throws Exception {
        LoanApplication app = new LoanApplication();
        app.setId(1L);
        app.setApplicantId("applicant-1");
        app.setStatus(LoanStatus.DRAFT);
        when(service.findActiveApplicationByApplicant("applicant-1", "durga@gmail.com")).thenReturn(app);
        when(service.getApplicantTimeline(1L, "applicant-1", "durga@gmail.com")).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/api/v1/applications/status")
                        .header("loggedInUser", "durga@gmail.com")
                        .header("applicantId", "applicant-1")
                        .header("userRole", "APPLICANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.timeline").isArray());
    }

    @Test
    void ensureApplicantRole_ShouldThrowException_WhenRoleIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/applications/history")
                        .header("loggedInUser", "durga@gmail.com")
                        .header("applicantId", "applicant-1")
                        .header("userRole", "ADMIN")) // Should be APPLICANT
                .andExpect(status().isInternalServerError()); // ApplicationException results in 500 by default global handler if not mapped
    }
}
