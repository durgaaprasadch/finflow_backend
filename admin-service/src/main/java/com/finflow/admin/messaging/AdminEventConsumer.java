package com.finflow.admin.messaging;

import com.finflow.admin.entity.AdminAudit;
import com.finflow.admin.repository.AdminAuditRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminEventConsumer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminEventConsumer.class);

    private final AdminAuditRepository auditRepository;
    
    public AdminEventConsumer(AdminAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @RabbitListener(queues = "application_submitted_queue")
    public void consumeApplicationSubmitted(Map<String, Object> applicationData) {
        log.info("Admin Service received APPLICATION_SUBMITTED event: {}", applicationData);
        AdminAudit audit = new AdminAudit();
        audit.setAction("APPLICATION_SUBMITTED");
        audit.setDetails("Received submission for Application ID: " + applicationData.get("id"));
        auditRepository.save(audit);
    }

    @RabbitListener(queues = "document_uploaded_queue")
    public void consumeDocumentUploaded(Map<String, Object> documentData) {
        log.info("Admin Service received DOCUMENT_UPLOADED event: {}", documentData);
        AdminAudit audit = new AdminAudit();
        audit.setAction("DOCUMENT_UPLOADED");
        audit.setDetails("Received document for Application ID: " + documentData.get("applicationId"));
        auditRepository.save(audit);
    }
}
