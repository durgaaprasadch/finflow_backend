package com.finflow.document.messaging;

import com.finflow.document.dto.DocumentMessage;
import com.finflow.document.entity.LoanDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class DocumentEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private DocumentEventPublisher eventPublisher;

    @Test
    void publishDocumentUploadedEvent_ShouldSendToRabbit() {
        // Setup reflection for Value annotations
        ReflectionTestUtils.setField(eventPublisher, "exchange", "test-exchange");
        ReflectionTestUtils.setField(eventPublisher, "routingKey", "test-key");

        LoanDocument doc = new LoanDocument();
        doc.setApplicationId(101L);
        doc.setDocumentId(202L);
        doc.setDocumentType(com.finflow.document.entity.DocumentType.AADHAAR);
        doc.setStatus(com.finflow.document.entity.DocumentStatus.UPLOADED);
        doc.setFileName("test.pdf");
        doc.setUserId("user1");

        eventPublisher.publishDocumentUploadedEvent(doc);

        verify(rabbitTemplate, times(1)).convertAndSend(eq("test-exchange"), eq("test-key"), any(DocumentMessage.class));
    }
}
