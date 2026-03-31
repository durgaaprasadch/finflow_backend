package com.finflow.admin.messaging;

import com.finflow.admin.repository.AdminAuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AdminEventConsumerTest {

    @Mock
    private AdminAuditRepository auditRepository;

    @InjectMocks
    private AdminEventConsumer adminEventConsumer;

    @Test
    void consumeApplicationSubmitted_ShouldSaveAudit() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 1L);

        adminEventConsumer.consumeApplicationSubmitted(data);

        verify(auditRepository, times(1)).save(any());
    }

    @Test
    void consumeDocumentUploaded_ShouldSaveAudit() {
        Map<String, Object> data = new HashMap<>();
        data.put("applicationId", 1L);

        adminEventConsumer.consumeDocumentUploaded(data);

        verify(auditRepository, times(1)).save(any());
    }
}
