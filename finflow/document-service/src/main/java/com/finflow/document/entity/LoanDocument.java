package com.finflow.document.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "loan_documents")
public class LoanDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    private String userId;

    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 50)
    private DocumentType documentType; // e.g. PASSPORT, PAYSLIP, BANK_STATEMENT

    private String fileName;

    private String fileType; // MIME type, e.g. application/pdf

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private DocumentStatus status; // UPLOADED, VERIFIED, REJECTED


    @Lob
    @JsonIgnore
    @Column(name = "document_data", columnDefinition = "LONGBLOB")
    private byte[] documentData;

    public LoanDocument() {
        // Default constructor for JPA
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public byte[] getDocumentData() { return documentData; }
    public void setDocumentData(byte[] documentData) { this.documentData = documentData; }
}
