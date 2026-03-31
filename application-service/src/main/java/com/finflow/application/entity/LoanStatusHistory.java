package com.finflow.application.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_status_history")
public class LoanStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private LoanStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private LoanStatus toStatus;

    private String changedBy;

    private LocalDateTime changedAt;

    private String reason;

    public LoanStatusHistory() {}

    public LoanStatusHistory(Long id, Long applicationId, LoanStatus fromStatus, LoanStatus toStatus, String changedBy, LocalDateTime changedAt, String reason) {
        this.id = id;
        this.applicationId = applicationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.reason = reason;
    }

    public static LoanStatusHistoryBuilder builder() {
        return new LoanStatusHistoryBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public LoanStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(LoanStatus fromStatus) { this.fromStatus = fromStatus; }
    public LoanStatus getToStatus() { return toStatus; }
    public void setToStatus(LoanStatus toStatus) { this.toStatus = toStatus; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public static class LoanStatusHistoryBuilder {
        private Long id;
        private Long applicationId;
        private LoanStatus fromStatus;
        private LoanStatus toStatus;
        private String changedBy;
        private LocalDateTime changedAt;
        private String reason;

        public LoanStatusHistoryBuilder id(Long id) { this.id = id; return this; }
        public LoanStatusHistoryBuilder applicationId(Long applicationId) { this.applicationId = applicationId; return this; }
        public LoanStatusHistoryBuilder fromStatus(LoanStatus fromStatus) { this.fromStatus = fromStatus; return this; }
        public LoanStatusHistoryBuilder toStatus(LoanStatus toStatus) { this.toStatus = toStatus; return this; }
        public LoanStatusHistoryBuilder changedBy(String changedBy) { this.changedBy = changedBy; return this; }
        public LoanStatusHistoryBuilder changedAt(LocalDateTime changedAt) { this.changedAt = changedAt; return this; }
        public LoanStatusHistoryBuilder reason(String reason) { this.reason = reason; return this; }

        public LoanStatusHistory build() {
            return new LoanStatusHistory(id, applicationId, fromStatus, toStatus, changedBy, changedAt, reason);
        }
    }
}
