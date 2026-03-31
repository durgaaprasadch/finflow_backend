package com.finflow.admin.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSummaryDTO {
    private Long applicationId;
    private String applicantId;
    private String applicantName;
    private String loanType;
    private BigDecimal requestedAmount;
    private String status;
    private LocalDateTime submittedAt;
}
