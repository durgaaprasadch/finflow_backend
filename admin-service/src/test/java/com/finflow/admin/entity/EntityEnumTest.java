package com.finflow.admin.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class EntityEnumTest {

    @Test
    void testEnums() {
        for (DocumentStatus status : DocumentStatus.values()) {
            assertNotNull(DocumentStatus.valueOf(status.name()));
        }
        for (com.finflow.admin.dto.LoanStatus status : com.finflow.admin.dto.LoanStatus.values()) {
            assertNotNull(com.finflow.admin.dto.LoanStatus.valueOf(status.name()));
        }
    }

    @Test
    void testAdminAudit() {
        AdminAudit audit = new AdminAudit();
        audit.setId(1L);
        audit.setAction("TEST");
        audit.setDetails("DETAILS");
        LocalDateTime now = LocalDateTime.now();
        audit.setTimestamp(now);
        
        assertEquals(1L, audit.getId());
        assertEquals("TEST", audit.getAction());
        assertEquals("DETAILS", audit.getDetails());
        assertEquals(now, audit.getTimestamp());
    }
}
