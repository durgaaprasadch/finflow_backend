package com.finflow.admin.controller;

import com.finflow.admin.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminWorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminWorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @Test
    void getSubmittedApplications_ShouldReturnWrappedResponse() throws Exception {
        when(adminService.getSubmittedApplications()).thenReturn(List.of(
                Map.of("id", 1L, "status", "SUBMITTED")
        ));
 
        mockMvc.perform(get("/api/v1/admin/applications/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("SUBMITTED"));
    }
 
    @Test
    void getApplicationDetailsByApplicant_ShouldReturnWrappedResponse() throws Exception {
        when(adminService.getApplicationDetailsForApplicant("app-1")).thenReturn(Map.of("id", 1L, "status", "SUBMITTED"));
 
        mockMvc.perform(get("/api/v1/admin/applications/app-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Application fetched successfully"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void getDocuments_ShouldReturnFilesResponse() throws Exception {
        when(adminService.getDocumentFiles(eq("app-1"), anyString(), anyString())).thenReturn(Map.of(
                "id", 1L,
                "documents", Map.of("AADHAAR", "base64")
        ));
 
        mockMvc.perform(get("/api/v1/admin/documents/app-1/files")
                        .header("loggedInUser", "admin@finflow.in")
                        .header("applicantId", "admin-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.documents.AADHAAR").value("base64"));
    }

    @Test
    void verifyDocuments_ShouldCallService() throws Exception {
        when(adminService.verifyDocuments(eq("app-1"), anyString(), anyString(), eq("VERIFIED"), anyString()))
                .thenReturn(Map.of("status", "DOCS_VERIFIED"));
 
        mockMvc.perform(patch("/api/v1/admin/documents/verify/app-1")
                        .header("loggedInUser", "admin@finflow.in")
                        .header("applicantId", "admin-1")
                        .param("userId", "user-123")
                        .param("status", "VERIFIED")
                        .param("remarks", "All good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DOCS_VERIFIED"));
    }

    @Test
    void decision_ShouldCallService() throws Exception {
        when(adminService.makeFinalDecision(eq("app-1"), eq("APPROVED"), anyString(), anyString(), anyString()))
                .thenReturn(Map.of("decision", "APPROVED"));
 
        mockMvc.perform(patch("/api/v1/admin/applications/decision/app-1")
                        .header("loggedInUser", "admin@finflow.in")
                        .header("applicantId", "admin-1")
                        .param("userId", "user-123")
                        .param("decision", "APPROVED")
                        .param("remarks", "Credit score is good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decision").value("APPROVED"));
    }

}
