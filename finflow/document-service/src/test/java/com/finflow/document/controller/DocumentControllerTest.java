package com.finflow.document.controller;

import com.finflow.document.entity.LoanDocument;
import com.finflow.document.entity.DocumentStatus;
import com.finflow.document.entity.DocumentType;
import com.finflow.document.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService service;

    private LoanDocument doc;

    @BeforeEach
    void setUp() {
        doc = new LoanDocument();
        doc.setDocumentId(5L);
        doc.setApplicationId(1L);
        doc.setDocumentType(DocumentType.AADHAAR);
        doc.setFileName("aadhaar.pdf");
        doc.setFileType("application/pdf");
        doc.setDocumentData("Dummy Content".getBytes());
        doc.setUserId("testuser");
        doc.setStatus(DocumentStatus.UPLOADED);
    }

    // ✅ UPLOAD TEST
    @Test
    void uploadDocument_ShouldReturnDocument() throws Exception {

        when(service.uploadDocument(any(MultipartFile.class), anyLong(), any(), any(), any()))
                .thenReturn(doc);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aadhaar.pdf",
                "application/pdf",
                "Dummy Content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents")
                .file(file)
                .param("applicationId", "1")
                .param("documentType", "AADHAAR")
                .header("loggedInUser", "testuser")
                .header("userRole", "APPLICANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("aadhaar.pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }

    // ✅ GET MY UPLOADS
    @Test
    void getMyUploads_ShouldReturnList() throws Exception {
        when(service.getDocumentsByUserId("testuser"))
                .thenReturn(Collections.singletonList(doc));

        mockMvc.perform(get("/api/documents/me")
                .header("loggedInUser", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("aadhaar.pdf"));
    }

    // ✅ GET DOCUMENTS - ADMIN SUCCESS
    @Test
    void getDocuments_ShouldReturnList_WhenAdmin() throws Exception {

        when(service.getDocumentsForApplication(1L))
                .thenReturn(Collections.singletonList(doc));

        mockMvc.perform(get("/api/documents/application/1")
                .header("userRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentType").value("AADHAAR"));
    }

    // ✅ GET DOCUMENTS - APPLICANT FORBIDDEN
    @Test
    void getDocuments_ShouldReturn403_WhenApplicant() throws Exception {
        mockMvc.perform(get("/api/documents/application/1")
                .header("userRole", "APPLICANT"))
                .andExpect(status().isForbidden());
    }

    // ✅ DOWNLOAD SUCCESS
    @Test
    void downloadDocument_ShouldReturnFileBytes() throws Exception {

        when(service.getDocumentById(5L))
                .thenReturn(Optional.of(doc));

        mockMvc.perform(get("/api/documents/5/content")
                .header("loggedInUser", "testuser"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"aadhaar.pdf\""))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes("Dummy Content".getBytes()));
    }

    // ✅ DOWNLOAD NOT FOUND
    @Test
    void downloadDocument_ShouldReturn404WhenNotFound() throws Exception {

        when(service.getDocumentById(99L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/documents/99/content"))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadDocument_ShouldReturn403WhenApplicantDownloadingOtherUserFile() throws Exception {
        when(service.getDocumentById(5L)).thenReturn(Optional.of(doc));

        mockMvc.perform(get("/api/documents/5/content")
                .header("loggedInUser", "wronguser")
                .header("userRole", "APPLICANT"))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadDocument_ShouldReturn403WhenApplicantDownloadingFileWithNullUserId() throws Exception {
        doc.setUserId(null);
        when(service.getDocumentById(5L)).thenReturn(Optional.of(doc));

        mockMvc.perform(get("/api/documents/5/content")
                .header("loggedInUser", "testuser")
                .header("userRole", "APPLICANT"))
                .andExpect(status().isForbidden());
    }

    // ✅ NULL HANDLING TEST
    @Test
    void downloadDocument_ShouldHandleNullFilenameAndType() throws Exception {
        LoanDocument nullDoc = new LoanDocument();
        nullDoc.setUserId("testuser");
        nullDoc.setDocumentData("No Name".getBytes());
        when(service.getDocumentById(7L))
                .thenReturn(Optional.of(nullDoc));

        mockMvc.perform(get("/api/documents/7/content")
                .header("loggedInUser", "testuser"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"document\""))
                .andExpect(header().string("Content-Type",
                        MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(content().bytes("No Name".getBytes()));
    }
}
