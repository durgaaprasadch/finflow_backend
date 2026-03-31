package com.finflow.application.service;

import com.finflow.application.dto.DocumentMessage;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.LoanStatus;
import com.finflow.application.entity.LoanStatusHistory;
import com.finflow.application.entity.DocumentRequirement;
import com.finflow.application.messaging.DocumentEventListener;
import com.finflow.application.repository.DocumentRequirementRepository;
import com.finflow.application.repository.LoanApplicationRepository;
import com.finflow.application.repository.LoanStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@SuppressWarnings("null")
class HardeningTest {

    @Autowired
    private LoanApplicationService applicationService;

    @Autowired
    private LoanApplicationRepository repository;

    @Autowired
    private LoanStatusHistoryRepository historyRepository;

    @Autowired
    private DocumentRequirementRepository requirementRepository;

    @Autowired
    private DocumentEventListener eventListener;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockBean
    private RestTemplate restTemplate;

    @org.junit.jupiter.api.BeforeEach
    void migrateSchema() {
        try {
            jdbcTemplate.execute("ALTER TABLE loan_applications MODIFY COLUMN status VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE loan_status_history MODIFY COLUMN from_status VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE loan_status_history MODIFY COLUMN to_status VARCHAR(50)");
        } catch (Exception e) {
            System.out.println("Migration failed: " + e.getMessage());
        }

        requirementRepository.deleteAll();
    }

    @Test
    void testAuditTrail_ShouldLogChanges() {
        LoanApplication app = new LoanApplication();
        app.setApplicantUsername("auditUser");
        app.setAmount(BigDecimal.valueOf(5000));
        app.setStatus(LoanStatus.DRAFT);
        app = repository.save(app);

        applicationService.updateStatus(app.getId(), LoanStatus.SUBMITTED, "APPLICANT", "auditUser");

        List<LoanStatusHistory> history = historyRepository.findByApplicationId(app.getId());
        assertFalse(history.isEmpty());
        assertEquals(LoanStatus.DRAFT, history.get(0).getFromStatus());
        assertEquals(LoanStatus.SUBMITTED, history.get(0).getToStatus());
    }

    @Test
    void testIdempotency_ShouldNotProcessSameEventTwice() {
        LoanApplication app = new LoanApplication();
        app.setApplicantUsername("idemUser");
        app.setLoanType("PERSONAL");
        app.setAmount(BigDecimal.valueOf(5000));
        app.setStatus(LoanStatus.SUBMITTED);
        app = repository.save(app);

        DocumentRequirement passport = new DocumentRequirement();
        passport.setLoanType("PERSONAL");
        passport.setDocumentType("PASSPORT");
        passport.setMandatory(true);
        requirementRepository.save(passport);

        DocumentRequirement payslip = new DocumentRequirement();
        payslip.setLoanType("PERSONAL");
        payslip.setDocumentType("PAYSLIP");
        payslip.setMandatory(true);
        requirementRepository.save(payslip);

        String eventId = UUID.randomUUID().toString();
        DocumentMessage message = new DocumentMessage();
        message.setEventId(eventId);
        message.setApplicationId(app.getId());

        when(restTemplate.exchange(any(String.class), any(org.springframework.http.HttpMethod.class), any(org.springframework.http.HttpEntity.class), eq(List.class)))
            .thenReturn(new org.springframework.http.ResponseEntity<>(List.of(
                    java.util.Map.of("documentType", "PASSPORT"),
                    java.util.Map.of("documentType", "PAYSLIP")
            ), org.springframework.http.HttpStatus.OK));

        eventListener.handleDocumentUploaded(message);
        
        LoanApplication updated = repository.findById(app.getId()).get();
        assertEquals(LoanStatus.UPLOADED, updated.getStatus());

        eventListener.handleDocumentUploaded(message);

        List<LoanStatusHistory> history = historyRepository.findByApplicationId(app.getId());
        long syncCount = history.stream().filter(h -> h.getToStatus() == LoanStatus.UPLOADED).count();
        assertEquals(1, syncCount, "Should only log status change once per eventId");
    }

    @Test
    void testOptimisticLocking_ShouldThrowExceptionOnConcurrentUpdate() {
        LoanApplication app = new LoanApplication();
        app.setApplicantUsername("lockUser");
        app.setAmount(BigDecimal.valueOf(5000));
        app.setStatus(LoanStatus.DRAFT);
        app = repository.save(app);
        
        LoanApplication appCopy1 = repository.findById(app.getId()).get();
        LoanApplication appCopy2 = repository.findById(app.getId()).get();

        appCopy1.setStatus(LoanStatus.SUBMITTED);
        repository.save(appCopy1);

        appCopy2.setStatus(LoanStatus.SUBMITTED);
        assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class, () -> {
            repository.save(appCopy2);
        });
    }
}
