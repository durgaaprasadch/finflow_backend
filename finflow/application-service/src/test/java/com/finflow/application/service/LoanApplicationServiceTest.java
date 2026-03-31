package com.finflow.application.service;

import com.finflow.application.exception.ApplicationException;

import com.finflow.application.dto.CreateApplicationRequest;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.LoanStatus;

import com.finflow.application.exception.InvalidTransitionException;
import com.finflow.application.messaging.ApplicationEventPublisher;
import com.finflow.application.repository.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.finflow.application.repository.LoanStatusHistoryRepository historyRepository;

    @Mock
    private com.finflow.application.repository.DocumentRequirementRepository requirementRepository;

    @InjectMocks
    private LoanApplicationService service;

    @Mock
    private org.springframework.web.client.RestTemplate restTemplate;

    private LoanApplication draftApp;

    @BeforeEach
    void setUp() {
        draftApp = new LoanApplication();
        draftApp.setId(1L);
        draftApp.setApplicantUsername("hello");
        draftApp.setAmount(java.math.BigDecimal.valueOf(10000.0));
        draftApp.setStatus(LoanStatus.DRAFT);
    }

    @Test
    void createDraft_ShouldSetStatusToDraftAndSave() {
        when(repository.save(any(LoanApplication.class))).thenReturn(draftApp);

        LoanApplication created = service.createDraft(new LoanApplication());

        assertEquals(LoanStatus.DRAFT, created.getStatus());
        verify(repository, times(1)).save(any(LoanApplication.class));
    }

    @Test
    void createApplicantDraft_ShouldSetApplicantFieldsAndDraftStatus() {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setLoanType(com.finflow.application.entity.LoanType.HOME_LOAN);
        request.setRequestedAmount(java.math.BigDecimal.valueOf(500000));
        when(repository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanApplication created = service.createApplicantDraft(request, "applicant-1", "durga@gmail.com");

        assertEquals("applicant-1", created.getApplicantId());
        assertEquals("durga@gmail.com", created.getApplicantUsername());
        assertEquals(LoanStatus.DRAFT, created.getStatus());
    }

    @Test
    void updateStatus_ShouldThrowException_WhenApplicationIsClosed() {
        draftApp.setStatus(LoanStatus.CLOSED);
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));

        InvalidTransitionException exception = assertThrows(InvalidTransitionException.class, () -> {
            service.updateStatus(1L, LoanStatus.APPROVED, "ADMIN", null);
        });
        assertTrue(exception.getMessage().contains("Application is CLOSED"));
        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_ShouldThrowException_WhenApplicantTriesToSetInvalidStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));

        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            service.updateStatus(1L, LoanStatus.APPROVED, "APPLICANT", "hello");
        });
        assertTrue(exception.getMessage().contains("Only ADMIN can set status"));
        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_ApplicantShouldBeAbleToSubmit() {
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        when(repository.save(any(LoanApplication.class))).thenReturn(draftApp);

        LoanApplication updated = service.updateStatus(1L, LoanStatus.SUBMITTED, "APPLICANT", "hello");

        assertEquals(LoanStatus.SUBMITTED, updated.getStatus());
        verify(eventPublisher, times(1)).publishApplicationSubmittedEvent(draftApp);
    }

    @Test
    void updateStatus_AdminShouldBeBoundByStateMachine() {
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp)); // Current: DRAFT
        
        // Admin trying to jump from DRAFT to APPROVED directly
        InvalidTransitionException exception = assertThrows(InvalidTransitionException.class, () -> {
            service.updateStatus(1L, LoanStatus.APPROVED, "ADMIN", null);
        });
        assertTrue(exception.getMessage().contains("Strict transitions policy violated"));
        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_AdminCanTransitionSubmittedToDocsPending() {
        draftApp.setStatus(LoanStatus.SUBMITTED);
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        when(repository.save(any(LoanApplication.class))).thenReturn(draftApp);

        LoanApplication updated = service.updateStatus(1L, LoanStatus.DOCS_PENDING, "ADMIN", null);

        assertEquals(LoanStatus.DOCS_PENDING, updated.getStatus());
        verify(repository, times(1)).save(draftApp);
    }

    @Test
    void updateStatus_AdminCanTransitionUploadedToUnderReview() {
        draftApp.setStatus(LoanStatus.UPLOADED);
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        when(repository.save(any(LoanApplication.class))).thenReturn(draftApp);

        LoanApplication updated = service.updateStatus(1L, LoanStatus.REVIEW, "ADMIN", null);

        assertEquals(LoanStatus.REVIEW, updated.getStatus());
    }

    @Test
    void isValidTransition_FromInitialStates() {
        // DRAFT -> SUBMITTED (Valid)
        verifyTransition(LoanStatus.DRAFT, LoanStatus.SUBMITTED, true);
        verifyTransition(LoanStatus.DRAFT, LoanStatus.DOCS_PENDING, false);

        // SUBMITTED -> DOCS_PENDING, UPLOADED, PARTIAL (Valid)
        verifyTransition(LoanStatus.SUBMITTED, LoanStatus.DOCS_PENDING, true);
        verifyTransition(LoanStatus.SUBMITTED, LoanStatus.UPLOADED, true);
        verifyTransition(LoanStatus.SUBMITTED, LoanStatus.PARTIAL, true);
        verifyTransition(LoanStatus.SUBMITTED, LoanStatus.REVIEW, true);
        verifyTransition(LoanStatus.SUBMITTED, LoanStatus.DOCS_VERIFIED, true);
    }

    @Test
    void isValidTransition_FromDocsPendingAndPartial() {
        // DOCS_PENDING -> UPLOADED, PARTIAL, REUPLOAD, REJECTED (Valid)
        verifyTransition(LoanStatus.DOCS_PENDING, LoanStatus.UPLOADED, true);
        verifyTransition(LoanStatus.DOCS_PENDING, LoanStatus.PARTIAL, true);
        verifyTransition(LoanStatus.DOCS_PENDING, LoanStatus.REUPLOAD, true);
        verifyTransition(LoanStatus.DOCS_PENDING, LoanStatus.REJECTED, true);
        verifyTransition(LoanStatus.DOCS_PENDING, LoanStatus.VERIFIED, false);

        // PARTIAL -> UPLOADED, REUPLOAD (Valid)
        verifyTransition(LoanStatus.PARTIAL, LoanStatus.UPLOADED, true);
        verifyTransition(LoanStatus.PARTIAL, LoanStatus.REUPLOAD, true);
        verifyTransition(LoanStatus.PARTIAL, LoanStatus.SUBMITTED, false);
    }

    @Test
    void isValidTransition_FromUploadedAndReview() {
        // UPLOADED -> REVIEW, REUPLOAD, FAIL (Valid)
        verifyTransition(LoanStatus.UPLOADED, LoanStatus.REVIEW, true);
        verifyTransition(LoanStatus.UPLOADED, LoanStatus.REUPLOAD, true);
        verifyTransition(LoanStatus.UPLOADED, LoanStatus.FAIL, true);
        verifyTransition(LoanStatus.UPLOADED, LoanStatus.APPROVED, false);

        // REVIEW -> VERIFIED, REUPLOAD, FAIL, REJECTED, DOCS_PENDING (Valid)
        verifyTransition(LoanStatus.REVIEW, LoanStatus.VERIFIED, true);
        verifyTransition(LoanStatus.REVIEW, LoanStatus.DOCS_VERIFIED, true);
        verifyTransition(LoanStatus.REVIEW, LoanStatus.REUPLOAD, true);
        verifyTransition(LoanStatus.REVIEW, LoanStatus.FAIL, true);
        verifyTransition(LoanStatus.REVIEW, LoanStatus.REJECTED, true);
        verifyTransition(LoanStatus.REVIEW, LoanStatus.DOCS_PENDING, true);
        verifyTransition(LoanStatus.REVIEW, LoanStatus.APPROVED, false);
    }

    @Test
    void isValidTransition_FromVerifiedAndReupload() {
        // VERIFIED -> APPROVED, REJECTED, REUPLOAD (Valid)
        verifyTransition(LoanStatus.VERIFIED, LoanStatus.APPROVED, true);
        verifyTransition(LoanStatus.VERIFIED, LoanStatus.REJECTED, true);
        verifyTransition(LoanStatus.VERIFIED, LoanStatus.REUPLOAD, true);
        verifyTransition(LoanStatus.VERIFIED, LoanStatus.REVIEW, false);
        verifyTransition(LoanStatus.DOCS_VERIFIED, LoanStatus.APPROVED, true);
        verifyTransition(LoanStatus.DOCS_VERIFIED, LoanStatus.REJECTED, true);
        verifyTransition(LoanStatus.DOCS_VERIFIED, LoanStatus.REUPLOAD, true);

        // REUPLOAD -> DOCS_PENDING, PARTIAL (Valid)
        verifyTransition(LoanStatus.REUPLOAD, LoanStatus.DOCS_PENDING, true);
        verifyTransition(LoanStatus.REUPLOAD, LoanStatus.PARTIAL, true);
        verifyTransition(LoanStatus.REUPLOAD, LoanStatus.UPLOADED, false);
    }

    @Test
    void isValidTransition_FromFinalStates() {
        // FAIL -> REUPLOAD, REJECTED (Valid)
        verifyTransition(LoanStatus.FAIL, LoanStatus.REUPLOAD, true);
        verifyTransition(LoanStatus.FAIL, LoanStatus.REJECTED, true);
        verifyTransition(LoanStatus.FAIL, LoanStatus.DRAFT, false);

        // APPROVED -> CLOSED (Valid)
        verifyTransition(LoanStatus.APPROVED, LoanStatus.CLOSED, true);
        verifyTransition(LoanStatus.APPROVED, LoanStatus.REVIEW, false);

        // REJECTED -> CLOSED (Valid)
        verifyTransition(LoanStatus.REJECTED, LoanStatus.CLOSED, true);
        verifyTransition(LoanStatus.REJECTED, LoanStatus.DRAFT, false);
    }

    private void verifyTransition(LoanStatus from, LoanStatus to, boolean expected) {
        LoanApplication app = new LoanApplication();
        app.setId(1L);
        app.setStatus(from);
        when(repository.findById(1L)).thenReturn(Optional.of(app));

        if (expected) {
            when(repository.save(any())).thenReturn(app);
            assertDoesNotThrow(() -> service.updateStatus(1L, to, "ADMIN", null));
        } else {
            assertThrows(InvalidTransitionException.class, () -> service.updateStatus(1L, to, "ADMIN", null));
        }
    }

    @Test
    void syncStatusAfterDocumentUpload_ShouldUpdateWhenPending() {
        draftApp.setStatus(LoanStatus.DOCS_PENDING);
        draftApp.setLoanType("PERSONAL");
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        com.finflow.application.entity.DocumentRequirement requirement = new com.finflow.application.entity.DocumentRequirement();
        requirement.setLoanType("PERSONAL");
        requirement.setDocumentType("PASSPORT");
        requirement.setMandatory(true);
        when(requirementRepository.findByLoanTypeIgnoreCaseAndMandatoryTrue("PERSONAL"))
                .thenReturn(java.util.Collections.singletonList(requirement));
        when(restTemplate.exchange(anyString(), any(), any(), eq(java.util.List.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        java.util.Collections.singletonList(java.util.Map.of("documentType", "PASSPORT")),
                        org.springframework.http.HttpStatus.OK));
        when(repository.save(any(LoanApplication.class))).thenReturn(draftApp);

        service.syncStatusAfterDocumentUpload(1L);

        assertEquals(LoanStatus.UPLOADED, draftApp.getStatus());
        verify(repository, times(1)).save(draftApp);
    }

    @Test
    void syncStatusAfterDocumentUpload_ShouldMoveApplicantFlowToDocumentsCompleted() {
        draftApp.setStatus(LoanStatus.LOAN_DETAILS_ADDED);
        draftApp.setApplicantId("applicant-1");
        draftApp.setLoanType("PERSONAL");
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        when(requirementRepository.findByLoanTypeIgnoreCaseAndMandatoryTrue("PERSONAL"))
                .thenReturn(java.util.Collections.emptyList());
        when(restTemplate.exchange(anyString(), any(), any(), eq(java.util.List.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        java.util.Arrays.asList(
                                java.util.Map.of("documentType", "AADHAAR"),
                                java.util.Map.of("documentType", "PAN"),
                                java.util.Map.of("documentType", "SALARY_SLIP"),
                                java.util.Map.of("documentType", "BANK_STATEMENT"),
                                java.util.Map.of("documentType", "PHOTO")),
                        org.springframework.http.HttpStatus.OK));

        service.syncStatusAfterDocumentUpload(1L);

        assertEquals(LoanStatus.DOCUMENTS_COMPLETED, draftApp.getStatus());
    }

    @Test
    void submitApplicantApplication_ShouldRequireDocumentsCompleted() {
        draftApp.setStatus(LoanStatus.DOCUMENTS_COMPLETED);
        draftApp.setApplicantId("applicant-1");
        draftApp.setApplicantUsername("hello");
        when(repository.findByApplicantUsername("hello")).thenReturn(java.util.Collections.singletonList(draftApp));
        when(repository.save(any(LoanApplication.class))).thenReturn(draftApp);

        LoanApplication submitted = service.submitApplicantApplication("applicant-1", "hello");

        assertEquals(LoanStatus.SUBMITTED, submitted.getStatus());
        verify(eventPublisher).publishApplicationSubmittedEvent(draftApp);
    }

    @Test
    void updateStatus_AdminCanTransitionUnderReviewToDocsVerified() {
        draftApp.setStatus(LoanStatus.REVIEW);
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        when(repository.save(any(LoanApplication.class))).thenReturn(draftApp);

        LoanApplication updated = service.updateStatus(1L, LoanStatus.DOCS_VERIFIED, "ADMIN", null);

        assertEquals(LoanStatus.DOCS_VERIFIED, updated.getStatus());
    }

    @Test
    void getApplicationById_ShouldReturnApp_WhenFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));

        LoanApplication found = service.getApplicationById(1L);

        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void getApplicationsByUser_ShouldReturnList() {
        when(repository.findByApplicantUsername("hello")).thenReturn(java.util.Collections.singletonList(draftApp));
        
        java.util.List<LoanApplication> list = service.getApplicationsByUser("hello");
        assertEquals(1, list.size());
    }

    @Test
    void getAllApplications_ShouldReturnList() {
        when(repository.findAll()).thenReturn(java.util.Collections.singletonList(draftApp));
        
        java.util.List<LoanApplication> list = service.getAllApplications();
        assertEquals(1, list.size());
    }

    @Test
    void getApplicationById_ShouldThrowRuntimeException_WhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getApplicationById(1L));
    }

    @Test
    void syncStatusAfterDocumentUpload_ShouldNotUpdate_WhenAlreadyUploaded() {
        draftApp.setStatus(LoanStatus.UPLOADED);
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));

        service.syncStatusAfterDocumentUpload(1L);

        verify(repository, never()).save(any());
    }

    @Test
    void syncStatusAfterDocumentUpload_ShouldStayAsPartial_WhenRequirementsNotMet() {
        draftApp.setStatus(LoanStatus.SUBMITTED);
        draftApp.setLoanType("PERSONAL");
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        com.finflow.application.entity.DocumentRequirement requirement = new com.finflow.application.entity.DocumentRequirement();
        requirement.setLoanType("PERSONAL");
        requirement.setDocumentType("PASSPORT");
        requirement.setMandatory(true);
        when(requirementRepository.findByLoanTypeIgnoreCaseAndMandatoryTrue("PERSONAL"))
                .thenReturn(java.util.Collections.singletonList(requirement));

        when(restTemplate.exchange(anyString(), any(), any(), eq(java.util.List.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        java.util.Collections.singletonList(java.util.Map.of("documentType", "BANK_STATEMENT")),
                        org.springframework.http.HttpStatus.OK));

        service.syncStatusAfterDocumentUpload(1L);

        assertEquals(LoanStatus.PARTIAL, draftApp.getStatus());
        verify(repository, times(1)).save(draftApp);
    }

    @Test
    void syncStatusAfterDocumentUpload_ShouldHandleRestTemplateException() {
        draftApp.setStatus(LoanStatus.SUBMITTED);
        draftApp.setLoanType("PERSONAL");
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        com.finflow.application.entity.DocumentRequirement requirement = new com.finflow.application.entity.DocumentRequirement();
        requirement.setLoanType("PERSONAL");
        requirement.setDocumentType("PASSPORT");
        requirement.setMandatory(true);
        when(requirementRepository.findByLoanTypeIgnoreCaseAndMandatoryTrue("PERSONAL"))
                .thenReturn(java.util.Collections.singletonList(requirement));

        when(restTemplate.exchange(anyString(), any(), any(), eq(java.util.List.class)))
                .thenThrow(new RuntimeException("API Error"));

        service.syncStatusAfterDocumentUpload(1L);

        // Should be PARTIAL because checkRequirementsMet returns false on exception
        assertEquals(LoanStatus.PARTIAL, draftApp.getStatus());
    }

    @Test
    void syncStatusAfterDocumentUpload_ShouldStayIncomplete_WhenNoRequirementsExist() {
        draftApp.setStatus(LoanStatus.DOCS_PENDING);
        draftApp.setLoanType("PERSONAL");
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        when(requirementRepository.findByLoanTypeIgnoreCaseAndMandatoryTrue("PERSONAL"))
                .thenReturn(java.util.Collections.emptyList());

        service.syncStatusAfterDocumentUpload(1L);

        assertEquals(LoanStatus.PARTIAL, draftApp.getStatus());
    }

    @Test
    void syncStatusAfterDocumentUpload_ShouldReturnFalse_WhenUploadedDocsIsNull() {
        draftApp.setStatus(LoanStatus.DOCS_PENDING);
        draftApp.setLoanType("PERSONAL");
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        com.finflow.application.entity.DocumentRequirement requirement = new com.finflow.application.entity.DocumentRequirement();
        requirement.setLoanType("PERSONAL");
        requirement.setDocumentType("PASSPORT");
        requirement.setMandatory(true);
        when(requirementRepository.findByLoanTypeIgnoreCaseAndMandatoryTrue("PERSONAL"))
                .thenReturn(java.util.Collections.singletonList(requirement));

        when(restTemplate.exchange(anyString(), any(), any(), eq(java.util.List.class)))
                .thenReturn(
                        new org.springframework.http.ResponseEntity<>(java.util.Collections.emptyList(), org.springframework.http.HttpStatus.OK));

        service.syncStatusAfterDocumentUpload(1L);

        assertEquals(LoanStatus.PARTIAL, draftApp.getStatus());
    }

    @Test
    void syncStatusAfterDocumentUpload_ShouldUpdateToUploaded_WhenExactlyTwoDocs() {
        draftApp.setStatus(LoanStatus.DOCS_PENDING);
        draftApp.setLoanType("PERSONAL");
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        com.finflow.application.entity.DocumentRequirement passport = new com.finflow.application.entity.DocumentRequirement();
        passport.setLoanType("PERSONAL");
        passport.setDocumentType("PASSPORT");
        passport.setMandatory(true);
        com.finflow.application.entity.DocumentRequirement payslip = new com.finflow.application.entity.DocumentRequirement();
        payslip.setLoanType("PERSONAL");
        payslip.setDocumentType("PAYSLIP");
        payslip.setMandatory(true);
        when(requirementRepository.findByLoanTypeIgnoreCaseAndMandatoryTrue("PERSONAL"))
                .thenReturn(java.util.Arrays.asList(passport, payslip));

        when(restTemplate.exchange(anyString(), any(), any(), eq(java.util.List.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        java.util.Arrays.asList(
                                java.util.Map.of("documentType", "PASSPORT"),
                                java.util.Map.of("documentType", "PAYSLIP")),
                        org.springframework.http.HttpStatus.OK));

        service.syncStatusAfterDocumentUpload(1L);

        assertEquals(LoanStatus.UPLOADED, draftApp.getStatus());
    }

    @Test
    void syncStatusAfterDocumentUpload_ShouldUpdateToUploaded_WhenMoreThanTwoDocs() {
        draftApp.setStatus(LoanStatus.PARTIAL);
        draftApp.setLoanType("PERSONAL");
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));
        com.finflow.application.entity.DocumentRequirement requirement = new com.finflow.application.entity.DocumentRequirement();
        requirement.setLoanType("PERSONAL");
        requirement.setDocumentType("PASSPORT");
        requirement.setMandatory(true);
        when(requirementRepository.findByLoanTypeIgnoreCaseAndMandatoryTrue("PERSONAL"))
                .thenReturn(java.util.Collections.singletonList(requirement));

        when(restTemplate.exchange(anyString(), any(), any(), eq(java.util.List.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        java.util.Arrays.asList(
                                java.util.Map.of("documentType", "PASSPORT"),
                                java.util.Map.of("documentType", "PAYSLIP"),
                                java.util.Map.of("documentType", "BANK_STATEMENT")),
                        org.springframework.http.HttpStatus.OK));

        service.syncStatusAfterDocumentUpload(1L);

        assertEquals(LoanStatus.UPLOADED, draftApp.getStatus());
    }

    @Test
    void updateStatus_ShouldThrowException_WhenApplicantTargetsAnotherUsersApplication() {
        when(repository.findById(1L)).thenReturn(Optional.of(draftApp));

        ApplicationException exception = assertThrows(ApplicationException.class, () ->
                service.updateStatus(1L, LoanStatus.SUBMITTED, "APPLICANT", "other-user"));

        assertTrue(exception.getMessage().contains("Applicants can only modify their own applications"));
        verify(repository, never()).save(any());
    }
}

