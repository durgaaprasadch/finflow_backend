package com.finflow.document.controller;

import com.finflow.document.entity.DocumentStatus;
import com.finflow.document.entity.DocumentType;
import com.finflow.document.entity.LoanDocument;
import com.finflow.document.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicantDocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApplicantDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @Test
    void uploadAllDocuments_ShouldReturnBulkUploadResponse() throws Exception {
        LoanDocument document = new LoanDocument();
        document.setApplicationId(1L);
        document.setDocumentId(10L);
        document.setDocumentType(DocumentType.AADHAAR);
        document.setStatus(DocumentStatus.UPLOADED);
        when(documentService.uploadAllRequiredDocuments(eq(1L), eq("applicant-1"), eq("APPLICANT"), any(), any(), any(),
                any(), any()))
                .thenReturn(List.of(document));

        MockMultipartFile aadhaar = new MockMultipartFile("aadhaarFile", "aadhaar.pdf", "application/pdf",
                "a".getBytes());
        MockMultipartFile pan = new MockMultipartFile("panFile", "pan.pdf", "application/pdf", "p".getBytes());
        MockMultipartFile salary = new MockMultipartFile("salarySlipFile", "salary.pdf", "application/pdf",
                "s".getBytes());
        MockMultipartFile bank = new MockMultipartFile("bankStatementFile", "bank.pdf", "application/pdf",
                "b".getBytes());
        MockMultipartFile photo = new MockMultipartFile("photoFile", "photo.jpg", "image/jpeg", "i".getBytes());

        mockMvc.perform(multipart("/api/v1/documents/upload-all/1")
                .file(aadhaar)
                .file(pan)
                .file(salary)
                .file(bank)
                .file(photo)
                .header("loggedInUser", "durga@gmail.com")
                .header("applicantId", "applicant-1")
                .header("userRole", "APPLICANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All documents uploaded successfully"))
                .andExpect(jsonPath("$.data.applicationId").value(1))
                .andExpect(jsonPath("$.data.status").value("DOCUMENTS_COMPLETED"));
    }

    @Test
    void getDocumentsZip_ShouldReturnZipFile() throws Exception {
        byte[] zipContent = "zip-data".getBytes();
        when(documentService.generateZipOfDocuments(1L)).thenReturn(zipContent);

        mockMvc.perform(get("/api/v1/documents/1")
                        .header("loggedInUser", "durga@gmail.com")
                        .header("applicantId", "applicant-1")
                        .header("userRole", "APPLICANT"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String contentType = result.getResponse().getContentType();
                    assert contentType != null && contentType.contains("application/zip");
                });
    }

    @Test
    void getDocumentsZip_ShouldReturnForbidden_WhenNotOwner() throws Exception {
        LoanDocument otherDoc = new LoanDocument();
        otherDoc.setUserId("other-user");
        when(documentService.getDocumentsForApplication(1L)).thenReturn(List.of(otherDoc));

        mockMvc.perform(get("/api/v1/documents/1")
                        .header("loggedInUser", "durga@gmail.com")
                        .header("applicantId", "applicant-1")
                        .header("userRole", "APPLICANT"))
                .andExpect(status().isForbidden());
    }
}
