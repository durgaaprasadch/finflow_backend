package com.finflow.application.repository;

import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByApplicantUsername(String applicantUsername);
    Optional<LoanApplication> findByApplicantUsernameAndStatus(String applicantUsername, LoanStatus status);
    List<LoanApplication> findByApplicantIdOrderByCreatedAtDesc(String applicantId);
    Optional<LoanApplication> findTopByApplicantIdOrderByCreatedAtDesc(String applicantId);
}
