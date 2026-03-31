package com.finflow.application.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_applications")
public class LoanApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private Long id;

    @Version
    private Long version;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String applicantUsername;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private String applicantId;

    @Column(length = 50)
    private String loanType; // e.g. PERSONAL, HOME, EDUCATION, VEHICLE

    private BigDecimal amount;

    private Integer tenure; // in months

    @Column(length = 500)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private LoanStatus status;

    private String fullName;

    private LocalDate dob;

    @Column(length = 20)
    private String gender;

    @Column(length = 30)
    private String maritalStatus;

    @Column(length = 20)
    private String panNumber;

    @Column(length = 20)
    private String aadhaarNumber;

    private String addressLine1;

    private String city;

    private String state;

    @Column(length = 15)
    private String pincode;

    @Column(length = 30)
    private String employmentType;

    private String companyName;

    private String designation;

    private BigDecimal monthlyIncome;

    private Integer experienceYears;

    private BigDecimal loanAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime submittedAt;

    public LoanApplication() {}

    @SuppressWarnings("java:S107") // Existing constructor with high parameter count
    public LoanApplication(Long id, Long version, String applicantUsername, String loanType,
                           BigDecimal amount, Integer tenure, String purpose, LoanStatus status) {
        this.id = id;
        this.version = version;
        this.applicantUsername = applicantUsername;
        this.loanType = loanType;
        this.amount = amount;
        this.tenure = tenure;
        this.purpose = purpose;
        this.status = status;
    }

    public static LoanApplicationBuilder builder() {
        return new LoanApplicationBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getApplicantUsername() { return applicantUsername; }
    public void setApplicantUsername(String applicantUsername) { this.applicantUsername = applicantUsername; }
    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }
    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getTenure() { return tenure; }
    public void setTenure(Integer tenure) { this.tenure = tenure; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
    public String getAadhaarNumber() { return aadhaarNumber; }
    public void setAadhaarNumber(String aadhaarNumber) { this.aadhaarNumber = aadhaarNumber; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
    public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static class LoanApplicationBuilder {
        private Long id;
        private Long version;
        private String applicantUsername;
        private String applicantId;
        private String loanType;
        private BigDecimal amount;
        private Integer tenure;
        private String purpose;
        private LoanStatus status;

        public LoanApplicationBuilder id(Long id) { this.id = id; return this; }
        public LoanApplicationBuilder version(Long version) { this.version = version; return this; }
        public LoanApplicationBuilder applicantUsername(String applicantUsername) { this.applicantUsername = applicantUsername; return this; }
        public LoanApplicationBuilder applicantId(String applicantId) { this.applicantId = applicantId; return this; }
        public LoanApplicationBuilder loanType(String loanType) { this.loanType = loanType; return this; }
        public LoanApplicationBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public LoanApplicationBuilder tenure(Integer tenure) { this.tenure = tenure; return this; }
        public LoanApplicationBuilder purpose(String purpose) { this.purpose = purpose; return this; }
        public LoanApplicationBuilder status(LoanStatus status) { this.status = status; return this; }

        public LoanApplication build() {
            LoanApplication loanApplication = new LoanApplication(id, version, applicantUsername, loanType, amount, tenure, purpose, status);
            loanApplication.setApplicantId(applicantId);
            return loanApplication;
        }
    }
}

