package com.finflow.application.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "document_requirements")
public class DocumentRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String loanType; // e.g., PERSONAL, HOME, VEHICLE

    private String documentType; // e.g., PASSPORT, SALARY_SLIP, ID_CARD

    private boolean mandatory;

    public DocumentRequirement() {
        // Default constructor for JPA
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
}
