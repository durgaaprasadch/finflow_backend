package com.finflow.document.service;

import com.finflow.document.entity.LoanDocument;
import com.finflow.document.entity.DocumentStatus;
import com.finflow.document.entity.DocumentType;
import com.finflow.document.messaging.DocumentEventPublisher;
import com.finflow.document.repository.LoanDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class DocumentServiceTest {

    @Mock
    private LoanDocumentRepository repository;

    @Mock
    private DocumentEventPublisher eventPublisher;

    @Mock
    private org.springframework.web.client.RestTemplate restTemplate;

    @InjectMocks
    private DocumentService service;

    private LoanDocument document;

    @BeforeEach
    void setUp() {
        document = new LoanDocument();
        document.setDocumentId(10L);
        document.setApplicationId(1L);
        document.setDocumentType(DocumentType.AADHAAR);
        document.setFileName("aadhaar.pdf");
        document.setFileType("application/pdf");
        document.setDocumentData("dummy content".getBytes());
        document.setStatus(DocumentStatus.UPLOADED);
    }

    @Test
    void uploadDocument_ShouldStoreInDBAndPublishEvent() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("aadhaar.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getBytes()).thenReturn("dummy content".getBytes());
        when(restTemplate.exchange(anyString(), any(), any(), eq(Object.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        java.util.Map.of("applicantUsername", "user1"),
                        org.springframework.http.HttpStatus.OK));

        when(repository.save(any(LoanDocument.class))).thenReturn(document);

        LoanDocument uploaded = service.uploadDocument(file, 1L, DocumentType.AADHAAR, "user1", "APPLICANT");

        assertEquals(DocumentStatus.UPLOADED, uploaded.getStatus());
        assertEquals("aadhaar.pdf", uploaded.getFileName());
        assertEquals("application/pdf", uploaded.getFileType());
        assertNotNull(uploaded.getDocumentData());
        verify(repository, times(1)).save(any(LoanDocument.class));
        verify(eventPublisher, times(1)).publishDocumentUploadedEvent(document);
    }

    @Test
    void getDocumentsForApplication_ShouldReturnList() {
        List<LoanDocument> list = new ArrayList<>();
        list.add(document);

        when(repository.findByApplicationId(1L)).thenReturn(list);

        List<LoanDocument> result = service.getDocumentsForApplication(1L);

        assertEquals(1, result.size());
        assertEquals(DocumentType.AADHAAR, result.get(0).getDocumentType());
    }

    @Test
    void getDocumentById_ShouldReturnDocument() {
        when(repository.findById(10L)).thenReturn(Optional.of(document));

        Optional<LoanDocument> result = service.getDocumentById(10L);

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getDocumentId());
    }

    @Test
    void getDocumentsByUserId_ShouldReturnList() {
        when(repository.findByUserId("user1")).thenReturn(java.util.Collections.singletonList(document));
        List<LoanDocument> result = service.getDocumentsByUserId("user1");
        assertEquals(1, result.size());
    }

    @Test
    void getDocumentByUserIdAndId_ShouldReturnDocument() {
        when(repository.findByUserIdAndDocumentId("user1", 10L)).thenReturn(Optional.of(document));
        Optional<LoanDocument> result = service.getDocumentByUserIdAndId("user1", 10L);
        assertTrue(result.isPresent());
    }

    @Test
    void uploadDocument_ShouldThrowDocumentException_WhenIOExceptionOccurs() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("error.pdf");
        when(file.getBytes()).thenThrow(new java.io.IOException("Disk Full"));
        when(restTemplate.exchange(anyString(), any(), any(), eq(Object.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        java.util.Map.of("applicantUsername", "user1"),
                        org.springframework.http.HttpStatus.OK));

        assertThrows(com.finflow.document.exception.DocumentException.class, () -> 
            service.uploadDocument(file, 1L, DocumentType.AADHAAR, "user1", "APPLICANT")
        );
    }

    @Test
    void uploadDocument_ShouldThrowDocumentException_WhenApplicantTargetsDifferentApplicationOwner() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Object.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        java.util.Map.of("applicantUsername", "different-user"),
                        org.springframework.http.HttpStatus.OK));

        assertThrows(com.finflow.document.exception.DocumentException.class, () ->
                service.uploadDocument(file, 1L, DocumentType.AADHAAR, "user1", "APPLICANT"));

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishDocumentUploadedEvent(any());
    }

    @Test
    void uploadDocument_ShouldAllowAdminBypassOwnershipVerification() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("aadhaar.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getBytes()).thenReturn("dummy content".getBytes());
        when(repository.save(any(LoanDocument.class))).thenReturn(document);

        LoanDocument uploaded = service.uploadDocument(file, 1L, DocumentType.AADHAAR, "admin", "ADMIN");

        assertEquals(DocumentStatus.UPLOADED, uploaded.getStatus());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(Object.class));
    }

    @Test
    void uploadDocument_ShouldThrowDocumentException_WhenOwnershipLookupFails() {
        MultipartFile file = mock(MultipartFile.class);
        when(restTemplate.exchange(anyString(), any(), any(), eq(Object.class)))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                        org.springframework.http.HttpStatus.NOT_FOUND));

        assertThrows(com.finflow.document.exception.DocumentException.class, () ->
                service.uploadDocument(file, 999L, DocumentType.AADHAAR, "user1", "APPLICANT"));

        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishDocumentUploadedEvent(any());
    }

    @Test
    void uploadAllRequiredDocuments_ShouldRejectWhenAnyFileMissing() {
        assertThrows(com.finflow.document.exception.DocumentException.class, () ->
                service.uploadAllRequiredDocuments(1L, "admin", "ADMIN", null, null, null, null, null));
    }

    @Test
    void generateZipOfDocuments_ShouldReturnZipData() {
        when(repository.findByApplicationId(1L)).thenReturn(java.util.Collections.singletonList(document));
        
        byte[] result = service.generateZipOfDocuments(1L);
        
        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
