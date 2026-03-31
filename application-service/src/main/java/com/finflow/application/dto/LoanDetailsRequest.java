package com.finflow.application.dto;

import java.math.BigDecimal;

public class LoanDetailsRequest {

    private BigDecimal loanAmount;
    private Integer tenureMonths;
    private com.finflow.application.entity.LoanType loanType;

    public com.finflow.application.entity.LoanType getLoanType() {
        return loanType;
    }

    public void setLoanType(com.finflow.application.entity.LoanType loanType) {
        this.loanType = loanType;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
    }
}
