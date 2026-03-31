package com.finflow.notification.listener;

import com.finflow.notification.dto.NotificationRequest;
import com.finflow.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EmailService emailService;

    @RabbitListener(queues = "${notification.queues.registration}")
    public void handleRegistrationNotification(NotificationRequest request) {
        log.info("RABBITMQ: Received REGISTRATION notification task for target: {}", request.getTo());
        if (request.getTemplateName() == null || request.getTemplateName().isEmpty()) {
            request.setTemplateName("registration-template");
        }
        emailService.sendEmail(request);
    }

    @RabbitListener(queues = "${notification.queues.loan-status}")
    public void handleLoanStatusNotification(NotificationRequest request) {
        log.info("RABBITMQ: Received LOAN_STATUS notification task for target: {}", request.getTo());
        if (request.getTemplateName() == null || request.getTemplateName().isEmpty()) {
            request.setTemplateName("loan-status-template");
        }
        emailService.sendEmail(request);
    }

    @RabbitListener(queues = "${notification.queues.login}")
    public void handleLoginNotification(NotificationRequest request) {
        log.info("RABBITMQ: Received LOGIN notification task for target: {}", request.getTo());
        if (request.getTemplateName() == null || request.getTemplateName().isEmpty()) {
            request.setTemplateName("login-template");
        }
        emailService.sendEmail(request);
    }
}
