package com.finflow.document.service;

import com.finflow.document.exception.DocumentException;

import com.finflow.document.entity.LoanDocument;
import com.finflow.document.messaging.DocumentEventPublisher;
import com.finflow.document.repository.LoanDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class DocumentService {

    private final LoanDocumentRepository repository;
    private final DocumentEventPublisher eventPublisher;
    private final RestTemplate restTemplate;

    public DocumentService(LoanDocumentRepository repository, DocumentEventPublisher eventPublisher, RestTemplate restTemplate) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.restTemplate = restTemplate;
    }

    private static final String APPLICATION_SERVICE_URL = "http://application-service/api/applications";

    public LoanDocument uploadDocument(MultipartFile file, Long applicationId, com.finflow.document.entity.DocumentType documentType, String userId, String role) {
        try {
            validateUploadAccess(applicationId, userId, role);
            return storeDocument(file, applicationId, documentType, userId);

        } catch (IOException e) {
            throw new DocumentException("Failed to store file in database: " + e.getMessage());
        }
    }

    @SuppressWarnings("java:S107") // Method requires multiple files to be uploaded simultaneously
    public List<LoanDocument> uploadAllRequiredDocuments(
            Long applicationId,
            String userId,
            String role,
            MultipartFile aadhaarFile,
            MultipartFile panFile,
            MultipartFile salarySlipFile,
            MultipartFile bankStatementFile,
            MultipartFile photoFile) {
        validateUploadAccess(applicationId, userId, role);

        List<MultipartFile> files = java.util.Arrays.asList(aadhaarFile, panFile, salarySlipFile, bankStatementFile, photoFile);
        if (files.stream().anyMatch(file -> file == null || file.isEmpty())) {
            throw new DocumentException("All required documents must be uploaded together.");
        }

        try {
            return List.of(
                    storeDocument(aadhaarFile, applicationId, com.finflow.document.entity.DocumentType.AADHAAR, userId),
                    storeDocument(panFile, applicationId, com.finflow.document.entity.DocumentType.PAN, userId),
                    storeDocument(salarySlipFile, applicationId, com.finflow.document.entity.DocumentType.SALARY_SLIP, userId),
                    storeDocument(bankStatementFile, applicationId, com.finflow.document.entity.DocumentType.BANK_STATEMENT, userId),
                    storeDocument(photoFile, applicationId, com.finflow.document.entity.DocumentType.PHOTO, userId)
            );
        } catch (IOException e) {
            throw new DocumentException("Failed to store uploaded documents: " + e.getMessage());
        }
    }


    private void validateUploadAccess(Long applicationId, String userId, String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }

        if (userId == null || userId.isBlank()) {
            throw new DocumentException("Unauthorized: Missing authenticated user");
        }

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("userRole", "ADMIN");
        org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

        Object body;
        try {
            body = restTemplate.exchange(
                    APPLICATION_SERVICE_URL + "/" + applicationId,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Object.class
            ).getBody();
        } catch (org.springframework.web.client.RestClientException ex) {
            throw new DocumentException("Unable to verify application ownership for the requested application.");
        }

        if (!(body instanceof Map<?, ?> map)) {
            throw new DocumentException("Unable to verify application ownership");
        }

        Object applicantUsername = map.get("applicantUsername");
        Object applicantId = map.get("applicantId");
        boolean matchesApplicantId = applicantId != null && userId.equalsIgnoreCase(applicantId.toString());
        boolean matchesApplicantUsername = applicantUsername != null && userId.equalsIgnoreCase(applicantUsername.toString());
        if (!matchesApplicantId && !matchesApplicantUsername) {
            throw new DocumentException("Unauthorized: You can only upload documents for your own applications.");
        }
    }

    public List<LoanDocument> getDocumentsForApplication(Long applicationId) {
        List<LoanDocument> docs = repository.findByApplicationId(applicationId);
        if (docs.isEmpty()) return docs;

        // Group by type and pick latest version of each
        return new java.util.ArrayList<>(docs.stream()
                .collect(java.util.stream.Collectors.toMap(
                        LoanDocument::getDocumentType,
                        d -> d,
                        (d1, d2) -> d1.getDocumentId() > d2.getDocumentId() ? d1 : d2
                )).values());
    }

    public byte[] generateZipOfDocuments(Long applicationId) {
        List<LoanDocument> latestDocs = getDocumentsForApplication(applicationId);
        if (latestDocs.isEmpty()) {
            return new byte[0];
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            java.util.Set<String> addedEntries = new java.util.HashSet<>();
            for (LoanDocument doc : latestDocs) {
                if (doc.getDocumentData() == null) continue;

                String extension = doc.getFileType() != null && doc.getFileType().contains("/") 
                    ? "." + doc.getFileType().split("/")[1] : "";
                
                String typeName = doc.getDocumentType() != null ? doc.getDocumentType().name().toLowerCase() : "document_" + doc.getDocumentId();
                String baseEntryName = typeName + extension;
                String entryName = baseEntryName;
                
                int counter = 1;
                while (addedEntries.contains(entryName)) {
                    entryName = typeName + "_" + counter + extension;
                    counter++;
                }
                addedEntries.add(entryName);
                
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(doc.getDocumentData());
                zos.closeEntry();
            }
        } catch (java.io.IOException e) {
            throw new DocumentException("Error generating ZIP for application: " + applicationId, e);
        }
        return baos.toByteArray();
    }

    public List<LoanDocument> getDocumentsByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public Optional<LoanDocument> getDocumentById(Long documentId) {
        return repository.findById(documentId);
    }

    public Optional<LoanDocument> getDocumentByAppAndType(Long applicationId, com.finflow.document.entity.DocumentType type) {
        return repository.findTopByApplicationIdAndDocumentTypeOrderByDocumentIdDesc(applicationId, type);
    }

    public Optional<LoanDocument> getDocumentByUserIdAndId(String userId, Long documentId) {
        return repository.findByUserIdAndDocumentId(userId, documentId);
    }

    private LoanDocument storeDocument(MultipartFile file, Long applicationId, com.finflow.document.entity.DocumentType documentType, String userId) throws IOException {
        LoanDocument document = new LoanDocument();
        document.setApplicationId(applicationId);
        document.setDocumentType(documentType);
        document.setUserId(userId);
        document.setFileName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setDocumentData(file.getBytes());
        document.setStatus(com.finflow.document.entity.DocumentStatus.UPLOADED);

        LoanDocument savedDoc = repository.save(document);
        eventPublisher.publishDocumentUploadedEvent(savedDoc);
        return savedDoc;
    }
}
