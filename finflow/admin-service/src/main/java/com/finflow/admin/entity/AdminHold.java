package com.finflow.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_holds")
public class AdminHold {

    public enum HoldStatus {
        ON_HOLD,
        RELEASED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String applicantId;

    @Enumerated(EnumType.STRING)
    private HoldStatus status;

    private String otp;
    private String reason;
    private String adminId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public AdminHold() {
        // Default constructor required by JPA
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }
    public HoldStatus getStatus() { return status; }
    public void setStatus(HoldStatus status) { this.status = status; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
