package com.finflow.document.repository;

import com.finflow.document.entity.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {
    List<LoanDocument> findByApplicationId(Long applicationId);
    List<LoanDocument> findByUserId(String userId);
    java.util.Optional<LoanDocument> findByUserIdAndDocumentId(String userId, Long documentId);
    java.util.Optional<LoanDocument> findTopByApplicationIdAndDocumentTypeOrderByDocumentIdDesc(Long applicationId, com.finflow.document.entity.DocumentType documentType);
}
