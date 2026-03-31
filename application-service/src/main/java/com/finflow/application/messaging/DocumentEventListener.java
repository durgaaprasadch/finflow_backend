package com.finflow.application.messaging;

import com.finflow.application.config.RabbitMQConfig;
import com.finflow.application.dto.DocumentMessage;
import com.finflow.application.entity.InboundEvent;
import com.finflow.application.repository.InboundEventRepository;
import com.finflow.application.exception.DocumentException;
import com.finflow.application.service.LoanApplicationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@SuppressWarnings("null")
public class DocumentEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DocumentEventListener.class);

    private final LoanApplicationService applicationService;
    private final InboundEventRepository inboundEventRepository;

    public DocumentEventListener(LoanApplicationService applicationService, InboundEventRepository inboundEventRepository) {
        this.applicationService = applicationService;
        this.inboundEventRepository = inboundEventRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_UPLOADED_QUEUE)
    public void handleDocumentUploaded(DocumentMessage message) {
        String eventId = message.getEventId();
        
        if (inboundEventRepository.existsById(eventId)) {
            log.warn("Duplicate event detected: {}. Skipping.", eventId);
            return;
        }

        try {
            Long applicationId = message.getApplicationId();
            log.info("Processing document upload event: {} for Application ID: {}", eventId, applicationId);
            
            applicationService.syncStatusAfterDocumentUpload(applicationId);

            // Mark as processed
            InboundEvent inboundEvent = new InboundEvent();
            inboundEvent.setEventId(eventId);
            inboundEvent.setProcessedAt(LocalDateTime.now());
            inboundEvent.setServiceName("application-service");
            inboundEventRepository.save(inboundEvent);

        } catch (Exception e) {
            throw new DocumentException("Failed to process document event: " + eventId, e); 
        }
    }
}
