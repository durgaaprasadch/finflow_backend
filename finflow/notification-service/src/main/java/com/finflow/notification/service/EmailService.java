package com.finflow.notification.service;

import com.finflow.notification.dto.NotificationRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.from:FinFlow <noreply@finflow.in>}")
    private String mailFrom;

    public void sendEmail(NotificationRequest request) {
        log.info("Sending email to {}", request.getTo());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            Context context = new Context();
            context.setVariables(request.getModel());

            String html = templateEngine.process(request.getTemplateName(), context);

            if (request.getTo() != null) helper.setTo(request.getTo());
            if (request.getSubject() != null) helper.setSubject(request.getSubject());
            if (html != null) helper.setText(html, true);
            if (mailFrom != null) helper.setFrom(mailFrom);

            mailSender.send(message);
            log.info("Email successfully sent to {}", request.getTo());
        } catch (jakarta.mail.MessagingException | org.thymeleaf.exceptions.TemplateEngineException e) {
            log.error("CRITICAL: Failed to process or send email to {}. Error: {}", request.getTo(), e.getMessage(), e);
            throw new com.finflow.notification.exception.NotificationException("Email delivery failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("GENERAL: Unexpected error sending email to {}", request.getTo(), e);
            throw new com.finflow.notification.exception.NotificationException("An unexpected error occurred during email delivery", e);
        }
    }
}
