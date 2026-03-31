package com.finflow.document.controller;

import com.finflow.document.dto.ApiResponse;
import com.finflow.document.entity.LoanDocument;
import com.finflow.document.service.DocumentService;
import com.finflow.document.client.ApplicationServiceClient;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@SuppressWarnings("null")
public class ApplicantDocumentController {

    private static final String ROLE_APPLICANT = "APPLICANT";
    private static final String HEADER_USER_ROLE = "userRole";
    private static final String HEADER_LOGGED_IN_USER = "loggedInUser";
    private static final String HEADER_APPLICANT_ID = "applicantId";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_APPLICATION_ID = "applicationId";
    private static final String DOCUMENTS_COMPLETED_STATUS = "DOCUMENTS_COMPLETED";

    private final DocumentService documentService;
    private final ApplicationServiceClient applicationServiceClient;

    public ApplicantDocumentController(DocumentService documentService, ApplicationServiceClient applicationServiceClient) {
        this.documentService = documentService;
        this.applicationServiceClient = applicationServiceClient;
    }

    @Tag(name = "1. KYC Submission", description = "One-step multi-part upload for all required identity and income proof")
    @Operation(summary = "Upload all required documents", description = "Registers documents for the specified application.")
    @PostMapping(value = "/upload-all", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> uploadAllDocuments(
            @RequestPart("aadhaarFile") MultipartFile aadhaarFile,
            @RequestPart("panFile") MultipartFile panFile,
            @RequestPart("salarySlipFile") MultipartFile salarySlipFile,
            @RequestPart("bankStatementFile") MultipartFile bankStatementFile,
            @RequestPart("photoFile") MultipartFile photoFile,
            HttpServletRequest request) {
        
        String username = request.getHeader(HEADER_LOGGED_IN_USER);
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String applicantId = request.getHeader(HEADER_APPLICANT_ID);
        
        Long resolvedAppId = applicationServiceClient.getActiveApplicationId(username, role, applicantId);
        if (resolvedAppId == null) {
            throw new com.finflow.document.exception.DocumentException("No active application found for your account.");
        }

        List<LoanDocument> uploaded = documentService.uploadAllRequiredDocuments(
                resolvedAppId,
                resolveApplicant(username, applicantId),
                role,
                aadhaarFile,
                panFile,
                salarySlipFile,
                bankStatementFile,
                photoFile);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put(FIELD_APPLICATION_ID, resolvedAppId);
        data.put(HEADER_APPLICANT_ID, applicantId);
        data.put(FIELD_STATUS, DOCUMENTS_COMPLETED_STATUS);
        data.put("documentsUploaded", uploaded.stream().map(doc -> doc.getDocumentType().name()).toList());
        return ApiResponse.success("All documents uploaded successfully", data);
    }

    @Tag(name = "2. Document Retrieval", description = "Securely fetch and download your uploaded KYC artifacts")
    @Operation(summary = "Download documents as ZIP", description = "Fetches all documents for the application in a single ZIP file.")
    @GetMapping(produces = "application/zip")
    public ResponseEntity<byte[]> getDocumentsZip(HttpServletRequest request) {
        String username = request.getHeader(HEADER_LOGGED_IN_USER);
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String applicantId = request.getHeader(HEADER_APPLICANT_ID);
        
        Long resolvedAppId = applicationServiceClient.getActiveApplicationId(username, role, applicantId);
        if (resolvedAppId == null) {
            return ResponseEntity.notFound().build();
        }

        return generateZipResponse(resolvedAppId);
    }

    @Hidden
    @GetMapping(value = "/internal/zip-all/{applicationId}", produces = "application/zip")
    public ResponseEntity<byte[]> getDocumentsZipInternal(
            @PathVariable("applicationId") Long applicationId,
            @RequestHeader(value = HEADER_USER_ROLE, defaultValue = ROLE_APPLICANT) String role) {
        
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return generateZipResponse(applicationId);
    }

    private ResponseEntity<byte[]> generateZipResponse(Long applicationId) {
        byte[] zipData = documentService.generateZipOfDocuments(applicationId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                .filename("application_" + applicationId + "_docs.zip").build());
        return new ResponseEntity<>(zipData, headers, HttpStatus.OK);
    }

    @Tag(name = "2. Document Retrieval")
    @Operation(summary = "Get list of uploaded files for application")
    @GetMapping("/files")
    public ApiResponse<Map<String, Object>> getUploadedFiles(HttpServletRequest request) {
        String username = request.getHeader(HEADER_LOGGED_IN_USER);
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String applicantId = request.getHeader(HEADER_APPLICANT_ID);
        
        Long resolvedAppId = applicationServiceClient.getActiveApplicationId(username, role, applicantId);
        if (resolvedAppId == null) {
            return ApiResponse.error("No active application found.", null);
        }

        return getFilesResponse(resolvedAppId);
    }

    @Hidden
    @GetMapping("/{applicationId}/files")
    public ApiResponse<Map<String, Object>> getUploadedFilesInternal(
            @PathVariable("applicationId") Long applicationId,
            @RequestHeader(value = HEADER_USER_ROLE, defaultValue = ROLE_APPLICANT) String role) {
        
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ApiResponse.error("Unauthorized", null);
        }
        return getFilesResponse(applicationId);
    }

    private ApiResponse<Map<String, Object>> getFilesResponse(Long applicationId) {
        List<LoanDocument> docs = documentService.getDocumentsForApplication(applicationId);
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, String> documents = new LinkedHashMap<>();
        for (LoanDocument doc : docs) {
            documents.put(doc.getDocumentType().name(), doc.getFileName());
        }
        result.put(FIELD_APPLICATION_ID, applicationId);
        result.put("documents", documents);
        return ApiResponse.success("Files retrieved", result);
    }

    @Tag(name = "2. Document Retrieval")
    @Operation(summary = "Download specific document")
    @GetMapping("/{type}")
    public ResponseEntity<byte[]> downloadDocumentByType(
            @io.swagger.v3.oas.annotations.Parameter(description = "Document type to download", 
                schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"AADHAAR", "PAN", "SALARY_SLIP", "BANK_STATEMENT", "PHOTO"}))
            @PathVariable("type") com.finflow.document.entity.DocumentType type,
            HttpServletRequest request) {
        
        String username = request.getHeader(HEADER_LOGGED_IN_USER);
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String applicantId = request.getHeader(HEADER_APPLICANT_ID);
        
        Long resolvedAppId = applicationServiceClient.getActiveApplicationId(username, role, applicantId);
        if (resolvedAppId == null) {
            return ResponseEntity.notFound().build();
        }

        return documentService.getDocumentByAppAndType(resolvedAppId, type).map(doc -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment()
                    .filename(doc.getFileName()).build());
            headers.setContentType(MediaType.parseMediaType(doc.getFileType()));
            return new ResponseEntity<>(doc.getDocumentData(), headers, HttpStatus.OK);
        }).orElse(ResponseEntity.notFound().build());
    }

    private String resolveApplicant(String userId, String applicantId) {
        return applicantId == null || applicantId.isBlank() ? userId : applicantId;
    }

    private String getHeader(HttpServletRequest request, String name, String defaultValue) {
        String val = request.getHeader(name);
        return val != null ? val : defaultValue;
    }
}
