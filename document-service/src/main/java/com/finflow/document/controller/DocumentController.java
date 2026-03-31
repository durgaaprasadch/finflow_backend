package com.finflow.document.controller;

import com.finflow.document.exception.DocumentException;

import com.finflow.document.entity.LoanDocument;
import com.finflow.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Hidden
@SuppressWarnings("null")
public class DocumentController {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_APPLICANT = "APPLICANT";
    private static final String HEADER_LOGGED_IN_USER = "loggedInUser";
    private static final String HEADER_USER_ROLE = "userRole";

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @io.swagger.v3.oas.annotations.Operation(tags = { "Document Upload Hub" })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LoanDocument uploadDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam("applicationId") Long applicationId,
            @RequestParam("documentType") com.finflow.document.entity.DocumentType documentType,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = HEADER_LOGGED_IN_USER, required = true) String userId,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = HEADER_USER_ROLE, defaultValue = ROLE_APPLICANT) String role) {
        return service.uploadDocument(file, applicationId, documentType, userId, role);
    }

    @io.swagger.v3.oas.annotations.Operation(tags = { "My Uploaded Documents" })
    @GetMapping("/me")
    public List<LoanDocument> getMyUploads(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = HEADER_LOGGED_IN_USER) String userId) {
        return service.getDocumentsByUserId(userId);
    }

    @io.swagger.v3.oas.annotations.Operation(tags = { "Admin: View Application Files" })
    @io.swagger.v3.oas.annotations.Hidden
    @GetMapping("/application/{applicationId}")
    public List<LoanDocument> getDocuments(
            @PathVariable("applicationId") Long applicationId,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = HEADER_USER_ROLE, defaultValue = ROLE_APPLICANT) String role) {

        if (!ROLE_ADMIN.equalsIgnoreCase(role)) {
            throw new DocumentException(
                    "Unauthorized: Application Document fetching must be done securely by an Admin.");
        }
        return service.getDocumentsForApplication(applicationId);
    }

    @io.swagger.v3.oas.annotations.Operation(tags = { "File Download" })
    @GetMapping("/{documentId}/content")
    public ResponseEntity<byte[]> downloadDocument(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = HEADER_LOGGED_IN_USER, required = false) String userId,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = HEADER_USER_ROLE, defaultValue = ROLE_APPLICANT) String role,
            @PathVariable("documentId") Long documentId) {

        return service.getDocumentById(documentId).map(doc -> {
            // ✅ ROLE CHECK: APPLICANT CAN ONLY DOWNLOAD OWN DOCUMENTS
            if (ROLE_APPLICANT.equalsIgnoreCase(role) && (doc.getUserId() == null || !doc.getUserId().equals(userId))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<byte[]>build();
            }
            HttpHeaders headers = new HttpHeaders();
            String fileName = doc.getFileName() != null ? doc.getFileName() : "document";
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment().filename(fileName).build());
            String contentType = doc.getFileType() != null ? doc.getFileType()
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;
            headers.setContentType(MediaType.parseMediaType(contentType));
            return new ResponseEntity<>(doc.getDocumentData(), headers, HttpStatus.OK);
        }).orElse(ResponseEntity.notFound().build());
    }

}
