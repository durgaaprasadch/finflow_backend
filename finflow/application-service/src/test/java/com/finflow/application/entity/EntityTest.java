package com.finflow.application.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Test
    void testLoanApplication() {
        LoanApplication app = LoanApplication.builder()
                .id(1L)
                .applicantUsername("user")
                .loanType("PERSONAL")
                .amount(BigDecimal.TEN)
                .tenure(24)
                .purpose("Home renovation")
                .status(LoanStatus.DRAFT)
                .version(1L)
                .build();
        
        assertEquals(1L, app.getId());
        assertEquals("user", app.getApplicantUsername());
        assertEquals("PERSONAL", app.getLoanType());
        assertEquals(BigDecimal.TEN, app.getAmount());
        assertEquals(24, app.getTenure());
        assertEquals("Home renovation", app.getPurpose());
        assertEquals(LoanStatus.DRAFT, app.getStatus());
        assertEquals(1L, app.getVersion());

        LoanApplication app2 = new LoanApplication();
        app2.setId(2L);
        app2.setLoanType("HOME");
        app2.setTenure(12);
        app2.setPurpose("Buy house");
        assertEquals(2L, app2.getId());
        assertEquals("HOME", app2.getLoanType());
        
        assertNotEquals(app, app2);
        // Verify toString-like coverage
        assertFalse(String.valueOf(app.getId()).isEmpty());
        assertNotEquals(0, app.hashCode());
    }


    @Test
    void testLoanStatusHistory() {
        LocalDateTime now = LocalDateTime.now();
        LoanStatusHistory history = LoanStatusHistory.builder()
                .id(1L)
                .applicationId(10L)
                .fromStatus(LoanStatus.DRAFT)
                .toStatus(LoanStatus.SUBMITTED)
                .changedBy("ADMIN")
                .changedAt(now)
                .reason("test")
                .build();
        
        assertEquals(1L, history.getId());
        assertEquals(10L, history.getApplicationId());
        assertEquals(LoanStatus.DRAFT, history.getFromStatus());
        assertEquals(LoanStatus.SUBMITTED, history.getToStatus());
        assertEquals("ADMIN", history.getChangedBy());
        assertEquals(now, history.getChangedAt());
        assertEquals("test", history.getReason());

        LoanStatusHistory history2 = new LoanStatusHistory();
        history2.setId(2L);
        assertNotEquals(history, history2);
    }

    @Test
    void testDocumentRequirement() {
        DocumentRequirement req = new DocumentRequirement();
        req.setId(1L);
        req.setLoanType("HOME");
        req.setDocumentType("PASSPORT");
        req.setMandatory(true);

        assertEquals(1L, req.getId());
        assertEquals("HOME", req.getLoanType());
        assertEquals("PASSPORT", req.getDocumentType());
        assertTrue(req.isMandatory());
    }

    @Test
    void testInboundEvent() {
        LocalDateTime now = LocalDateTime.now();
        InboundEvent event = new InboundEvent();
        event.setEventId("evt-1");
        event.setProcessedAt(now);
        event.setServiceName("test-service");

        assertEquals("evt-1", event.getEventId());
        assertEquals(now, event.getProcessedAt());
        assertEquals("test-service", event.getServiceName());
    }
    
    @Test
    void testLoanStatusEnum() {
        assertNotNull(LoanStatus.valueOf("DRAFT"));
        assertEquals(LoanStatus.DRAFT, LoanStatus.values()[0]);
    }
}
