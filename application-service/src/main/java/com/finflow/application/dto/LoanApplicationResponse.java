package com.finflow.application.dto;

import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {
    private Long id;
    private String applicantUsername;
    private String applicantId;
    private String loanType;
    private BigDecimal amount;
    private Integer tenure;
    private String purpose;
    private LoanStatus status;
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String maritalStatus;
    private String panNumber;
    private String aadhaarNumber;
    private String addressLine1;
    private String city;
    private String state;
    private String pincode;
    private String employmentType;
    private String companyName;
    private String designation;
    private BigDecimal monthlyIncome;
    private Integer experienceYears;
    private BigDecimal loanAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;

    public static LoanApplicationResponse fromEntity(LoanApplication entity) {
        if (entity == null) return null;
        return LoanApplicationResponse.builder()
                .id(entity.getId())
                .applicantUsername(entity.getApplicantUsername())
                .applicantId(entity.getApplicantId())
                .loanType(entity.getLoanType())
                .amount(entity.getAmount())
                .tenure(entity.getTenure())
                .purpose(entity.getPurpose())
                .status(entity.getStatus())
                .fullName(entity.getFullName())
                .dob(entity.getDob())
                .gender(entity.getGender())
                .maritalStatus(entity.getMaritalStatus())
                .panNumber(entity.getPanNumber())
                .aadhaarNumber(entity.getAadhaarNumber())
                .addressLine1(entity.getAddressLine1())
                .city(entity.getCity())
                .state(entity.getState())
                .pincode(entity.getPincode())
                .employmentType(entity.getEmploymentType())
                .companyName(entity.getCompanyName())
                .designation(entity.getDesignation())
                .monthlyIncome(entity.getMonthlyIncome())
                .experienceYears(entity.getExperienceYears())
                .loanAmount(entity.getLoanAmount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }
}
