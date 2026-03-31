package com.finflow.application.repository;

import com.finflow.application.entity.DocumentRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentRequirementRepository extends JpaRepository<DocumentRequirement, Long> {
    List<DocumentRequirement> findByLoanType(String loanType);
    List<DocumentRequirement> findByLoanTypeIgnoreCaseAndMandatoryTrue(String loanType);
}
