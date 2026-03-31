package com.finflow.document.messaging;

import com.finflow.document.dto.DocumentMessage;
import com.finflow.document.entity.LoanDocument;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class DocumentEventPublisher {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DocumentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public DocumentEventPublisher(
            RabbitTemplate rabbitTemplate, 
            @Value("${rabbitmq.exchange}") String exchange, 
            @Value("${rabbitmq.routing.key.uploaded}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publishDocumentUploadedEvent(LoanDocument doc) {
        DocumentMessage message = new DocumentMessage();
        message.setEventId(UUID.randomUUID().toString());
        message.setApplicationId(doc.getApplicationId());
        message.setDocumentId(doc.getDocumentId());
        message.setDocumentType(doc.getDocumentType().toString());
        message.setFileName(doc.getFileName());
        message.setUploadedBy(doc.getUserId());
        message.setTimestamp(java.time.LocalDateTime.now().toString());

        log.info("Publishing DocumentMessage: {} to {} with key {}", message.getEventId(), exchange, routingKey);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
