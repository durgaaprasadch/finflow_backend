package com.finflow.admin.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.finflow.admin.entity.AdminAudit;
import com.finflow.admin.exception.AdminException;
import com.finflow.admin.repository.AdminAuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class AdminService {

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_APPLICATION_ID = "applicationId";
    private static final String FIELD_APPLICANT_ID = "applicantId";
    private static final String FIELD_SUBMITTED_AT = "submittedAt";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String DECISION_APPROVE = "APPROVE";
    private static final String DECISION_REJECT = "REJECT";
    private static final String ROLE_ADMIN_LITERAL = "ADMIN";
    private static final String STATUS_DOCS_VERIFIED = "DOCS_VERIFIED";
    private static final String STATUS_REVIEW = "REVIEW";
    private static final String STATUS_UNVERIFIED = "UNVERIFIED";



    private static final Set<String> REQUIRED_DOCUMENT_TYPES = Set.of(
            "AADHAAR", "PAN", "SALARY_SLIP", "BANK_STATEMENT", "PHOTO");

    private static final String STATUS_QUERY_PARAM = "/status?status=";
    private static final String FIELD_APPLICANT_USERNAME = "applicantUsername";
    private static final String FIELD_FULL_NAME = "fullName";
    private static final String FIELD_MESSAGE = "message";

    private final RestTemplate restTemplate;
    private final AdminAuditRepository adminAuditRepository;
    private final com.finflow.admin.repository.AdminHoldRepository adminHoldRepository;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;
    private final java.util.Random random = new java.util.Random();

    private final String notificationExchange;
    private final String loanStatusRoutingKey;

    public AdminService(RestTemplate restTemplate, AdminAuditRepository adminAuditRepository,
                        com.finflow.admin.repository.AdminHoldRepository adminHoldRepository,
                        org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate,
                        @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.notification.exchanges.notification}") String notificationExchange,
                        @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.notification.routing-keys.loan-status}") String loanStatusRoutingKey) {
        this.restTemplate = restTemplate;
        this.adminAuditRepository = adminAuditRepository;
        this.adminHoldRepository = adminHoldRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.notificationExchange = notificationExchange;
        this.loanStatusRoutingKey = loanStatusRoutingKey;
    }

    // Fixed for Eureka Client load balancing
    private static final String APP_SERVICE_URL = "http://application-service/api/applications";
    private static final String DOC_SERVICE_URL = "http://document-service/api/documents";
    private static final String V1_DOC_SERVICE_URL = "http://document-service/api/v1/documents";
    private static final String AUTH_INTERNAL_URL = "http://auth-service/api/auth/internal/users";

    private HttpEntity<?> getAdminEntity() {
        return getAdminEntity(null, null);
    }

    private HttpEntity<?> getAdminEntity(String adminEmail, String adminId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("userRole", ROLE_ADMIN_LITERAL);
        headers.set("loggedInUser", adminEmail == null || adminEmail.isBlank() ? "admin-service" : adminEmail);
        if (adminId != null && !adminId.isBlank()) {
            headers.set(FIELD_APPLICANT_ID, adminId);
        }
        return new HttpEntity<>(headers);
    }

    @CircuitBreaker(name = "applicationService", fallbackMethod = "fallbackApplicationStatus")
    @Retry(name = "applicationService")
    @SuppressWarnings({ "unchecked", "null" })
    public Map<String, Object> getApplicationStatus(Long id) {
        ResponseEntity<Object> response = restTemplate.exchange(APP_SERVICE_URL + "/" + id, HttpMethod.GET,
                getAdminEntity(), Object.class);
        Object body = response.getBody();
        return body instanceof Map ? (Map<String, Object>) body : java.util.Collections.emptyMap();
    }

    @CircuitBreaker(name = "applicationService")
    @SuppressWarnings("null")
    public Object getAllApplications() {
        return restTemplate.exchange(APP_SERVICE_URL + "/all", HttpMethod.GET, getAdminEntity(), Object.class)
                .getBody();
    }

    @CircuitBreaker(name = "applicationService")
    @SuppressWarnings("null")
    public Object updateApplicationStatus(Long id, String status) {
        String cleanStatus = status.contains(". ") ? status.split("\\. ")[1] : status;
        return restTemplate.exchange(APP_SERVICE_URL + "/" + id + STATUS_QUERY_PARAM + cleanStatus, HttpMethod.PATCH,
                getAdminEntity(), Object.class).getBody();
    }

    @CircuitBreaker(name = "applicationService")
    @SuppressWarnings({ "unchecked", "null" })
    private Object updateApplicationStatus(Long id, String status, String adminEmail, String adminId) {
        String url = APP_SERVICE_URL + "/" + id + STATUS_QUERY_PARAM + status;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.PATCH,
                getAdminEntity(adminEmail, adminId), Object.class);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        return body != null ? body.get("data") : null;
    }

    @CircuitBreaker(name = "documentService")
    @SuppressWarnings("null")
    public Object getDocumentsForApplication(Long id) {
        return restTemplate
                .exchange(DOC_SERVICE_URL + "/application/" + id, HttpMethod.GET, getAdminEntity(), Object.class)
                .getBody();
    }

    @CircuitBreaker(name = "documentService")
    @SuppressWarnings("null")
    public org.springframework.http.ResponseEntity<byte[]> downloadDocument(Long documentId) {
        return restTemplate.exchange(DOC_SERVICE_URL + "/" + documentId + "/content", HttpMethod.GET, getAdminEntity(),
                byte[].class);
    }

    @SuppressWarnings("unchecked")
    public List<Object> getSubmittedApplications() {
        Object allApplications = getAllApplications();
        Map<String, Object> responseMap = (allApplications instanceof Map) ? (Map<String, Object>) allApplications
                : null;
        Object data = (responseMap != null && responseMap.containsKey("data")) ? responseMap.get("data")
                : allApplications;

        if (!(data instanceof List<?> list)) {
            return java.util.Collections.emptyList();
        }

        return list.stream()
                .filter(Map.class::isInstance)
                .map(obj -> (Map<String, Object>) obj)
                .filter(map -> STATUS_SUBMITTED.equals(String.valueOf(map.get(FIELD_STATUS))))
                .sorted(Comparator.comparing(map -> parseDateTime(map.get(FIELD_SUBMITTED_AT)),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(java.util.stream.Collectors.toList());
    }

    private java.time.LocalDateTime parseDateTime(Object value) {
        if (value == null)
            return null;
        try {
            return java.time.LocalDateTime.parse(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> getApplicationDetailsV1(Long id) {
        Map<String, Object> body = getApplicationStatus(id);
        if (body != null) {
            return body;
        }
        return java.util.Collections.emptyMap();
    }

    @SuppressWarnings({ "unchecked", "null" })
    public Map<String, Object> getApplicationDetailsForApplicant(String applicantId) {
        // Fetch application by applicantId instead of applicationId
        ResponseEntity<Object> response = restTemplate.exchange(APP_SERVICE_URL + "/applicant/" + applicantId,
                HttpMethod.GET, getAdminEntity(), Object.class);
        Object body = response.getBody();
        Map<String, Object> data = body instanceof Map ? (Map<String, Object>) body : java.util.Collections.emptyMap();

        if (data.isEmpty()) {
            throw new AdminException("No application found for applicantId: " + applicantId);
        }

        // Add internal hold status
        adminHoldRepository.findByApplicantId(applicantId).ifPresent(hold -> {
            data.put("internalHoldStatus", hold.getStatus().name());
            data.put("holdReason", hold.getReason());
        });

        return data;
    }

    public Map<String, Object> getDocumentFiles(String applicantId) {
        return getDocumentFiles(applicantId, null, null);
    }

    @SuppressWarnings("null")
    public Map<String, Object> getDocumentFiles(String applicantId, String adminEmail, String adminId) {
        // Validate applicant ownership
        Map<String, Object> application = getApplicationDetailsForApplicant(applicantId);
        Long applicationId = application.get("id") != null ? Long.valueOf(String.valueOf(application.get("id"))) : null;

        Object body = restTemplate.exchange(
                V1_DOC_SERVICE_URL + "/" + applicationId + "/files",
                HttpMethod.GET,
                getAdminEntity(adminEmail, adminId),
                Object.class).getBody();
        return unwrapDataMap(body);
    }

    @SuppressWarnings("null")
    public byte[] downloadAllDocumentsZip(String applicantId, String adminEmail, String adminId) {
        // First get application to get its ID (internal reference)
        Map<String, Object> application = getApplicationDetailsForApplicant(applicantId);
        if (application == null || application.isEmpty() || application.get("id") == null) {
            log.error("ADMIN_DOC_DOWNLOAD: No active application found for applicant {}. Download aborted.", applicantId);
            throw new AdminException("No active application found for this applicant to download documents.");
        }
        Long applicationId = Long.valueOf(String.valueOf(application.get("id")));

        ResponseEntity<byte[]> response = restTemplate.exchange(
                V1_DOC_SERVICE_URL + "/internal/zip-all/" + applicationId,
                HttpMethod.GET,
                getAdminEntity(adminEmail, adminId),
                byte[].class);
        return response.getBody();
    }

    public Object moveToReview(Long applicationId) {
        return updateApplicationStatus(applicationId, STATUS_REVIEW);
    }

    public Map<String, Object> verifyDocuments(String applicantId, String adminId, String adminEmail, String status,
            String remarks) {
        // Validate applicant ownership
        Map<String, Object> application = getApplicationDetailsForApplicant(applicantId);
        Long applicationId = application.get("id") != null ? Long.valueOf(String.valueOf(application.get("id"))) : null;

        validateStatusForVerification(String.valueOf(application.get(FIELD_STATUS)));

        validateDocumentCompleteness(applicantId, adminEmail, adminId);

        String targetStatus = STATUS_UNVERIFIED.equalsIgnoreCase(status) ? STATUS_UNVERIFIED : STATUS_DOCS_VERIFIED;
        Object updateResult = updateApplicationStatus(applicationId, targetStatus, adminEmail, adminId);
        auditAction(adminId, applicationId, targetStatus, remarks);

        Map<String, Object> response = asMap(updateResult);
        response.putIfAbsent(FIELD_APPLICATION_ID, applicationId);
        response.put(FIELD_STATUS, targetStatus);
        response.put("verifiedAt", java.time.LocalDateTime.now());
        return response;
    }

    private void validateStatusForVerification(String currentStatus) {
        if (!STATUS_SUBMITTED.equalsIgnoreCase(currentStatus) && !STATUS_REVIEW.equalsIgnoreCase(currentStatus)) {
            throw new AdminException("Documents can only be verified for submitted/review applications");
        }
    }

    private void validateDocumentCompleteness(String applicantId, String adminEmail,
            String adminId) {
        Map<String, Object> filesResponse = getDocumentFiles(applicantId, adminEmail, adminId);
        Object documentsObject = filesResponse.get("documents");
        if (!(documentsObject instanceof Map<?, ?> documentsMap)) {
            throw new AdminException("Unable to load uploaded documents for verification");
        }

        Set<String> uploadedTypes = documentsMap.keySet().stream()
                .map(String::valueOf)
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());
        if (!uploadedTypes.containsAll(REQUIRED_DOCUMENT_TYPES)) {
            throw new AdminException("All required documents must be present before verification");
        }
    }

    public Map<String, Object> makeFinalDecision(String applicantId, String decision, String adminId, String adminEmail,
            String remarks) {
        // Validate and get application
        Map<String, Object> application = getApplicationDetailsForApplicant(applicantId);
        Long applicationId = application.get("id") != null ? Long.valueOf(String.valueOf(application.get("id"))) : null;

        String normalizedDecision = normalizeDecision(decision);
        if (normalizedDecision.isBlank()) {
            throw new AdminException("Allowed decisions: " + DECISION_APPROVE + ", " + DECISION_REJECT);
        }

        Object updateResult = updateApplicationStatus(applicationId, normalizedDecision, adminEmail, adminId);
        auditAction(adminId, applicationId, normalizedDecision, remarks);

        Map<String, Object> response = asMap(updateResult);
        response.putIfAbsent(FIELD_APPLICATION_ID, applicationId);
        response.put("decision", normalizedDecision);
        response.put("remarks", remarks);
        response.put("decidedAt", java.time.LocalDateTime.now());

        // Notify the user of the final decision
        try {
            String applicantUsername = (String) application.get(FIELD_APPLICANT_USERNAME);
            String fullName = (String) application.get(FIELD_FULL_NAME);

            com.finflow.notification.dto.NotificationRequest notification = com.finflow.notification.dto.NotificationRequest
                    .builder()
                    .to(applicantUsername)
                    .subject("FinFlow: Final Decision on your Loan Application")
                    .templateName("loan-status-template")
                    .model(java.util.Map.of(
                            "name", fullName != null ? fullName : "Valued Customer",
                            FIELD_APPLICATION_ID, "APP-" + applicationId,
                            FIELD_STATUS, normalizedDecision.equals(STATUS_APPROVED) ? STATUS_APPROVED : STATUS_REJECTED))
                    .build();

            rabbitTemplate.convertAndSend(notificationExchange, loanStatusRoutingKey, notification);
            log.info("Queued final decision notification for user: {}", applicantUsername);
        } catch (Exception e) {
            log.error("Failed to queue final decision notification for applicant {}: {}", applicantId, e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getMyReport(String adminId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("adminId", adminId);
        response.put("totalReviewed", adminAuditRepository.countByAdminIdAndActionIn(adminId,
                List.of(STATUS_DOCS_VERIFIED, STATUS_APPROVED, STATUS_REJECTED)));
        response.put("approved", adminAuditRepository.countByAdminIdAndAction(adminId, STATUS_APPROVED));
        response.put("rejected", adminAuditRepository.countByAdminIdAndAction(adminId, STATUS_REJECTED));
        return response;
    }

    // Fallback Method executes if the Application Service is offline or times out
    // repeatedly
    public Object fallbackApplicationStatus(Long id, Throwable t) {
        log.error("Circuit Breaker activated! Fallback executing for App ID: {} due to: {}", id, t.getMessage());
        java.util.Map<String, Object> fallback = new java.util.HashMap<>();
        fallback.put(FIELD_MESSAGE, "Application Service is currently down. Try again later.");
        fallback.put("fallback", true);
        fallback.put(FIELD_APPLICATION_ID, id);
        return fallback;
    }

    private void auditAction(String adminId, Long applicationId, String action, String remarks) {
        AdminAudit audit = new AdminAudit();
        audit.setAdminId(adminId);
        audit.setApplicationId(applicationId);
        audit.setAction(action);
        audit.setDetails(remarks);
        audit.setTimestamp(java.time.LocalDateTime.now());
        adminAuditRepository.save(audit);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapDataMap(Object body) {
        if (!(body instanceof Map<?, ?> map)) {
            return java.util.Collections.emptyMap();
        }
        Object data = map.containsKey("data") ? map.get("data") : body;
        if (data instanceof Map<?, ?> dataMap) {
            return new LinkedHashMap<>((Map<String, Object>) dataMap);
        }
        return java.util.Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new LinkedHashMap<>();
    }

    private String normalizeDecision(String decision) {
        String normalized = decision == null ? "" : decision.trim().toUpperCase();
        if (DECISION_APPROVE.equals(normalized) || STATUS_APPROVED.equals(normalized)) {
            return STATUS_APPROVED;
        }
        if (DECISION_REJECT.equals(normalized) || STATUS_REJECTED.equals(normalized)) {
            return STATUS_REJECTED;
        }
        return "";
    }

    // INTERNAL ADMIN USER CONTROLS (Local State in Admin Service)
    @Transactional
    public Object requestHold(String applicantId) {
        String otp = String.format("%06d", random.nextInt(999999));
        
        com.finflow.admin.entity.AdminHold hold = adminHoldRepository.findByApplicantId(applicantId)
                .orElse(new com.finflow.admin.entity.AdminHold());
        
        hold.setApplicantId(applicantId);
        hold.setStatus(com.finflow.admin.entity.AdminHold.HoldStatus.RELEASED); // Not yet active
        hold.setOtp(otp);
        adminHoldRepository.save(hold);

        log.info("INTERNAL_HOLD: OTP {} generated for applicantId {}", otp, applicantId);
        return Map.of(FIELD_MESSAGE, "Internal hold OTP generated", "otp", otp);
    }

    @Transactional
    @SuppressWarnings("null")
    public Object verifyHold(String applicantId, String otp, String adminId, String adminEmail, String remarks) {
        com.finflow.admin.entity.AdminHold hold = adminHoldRepository.findByApplicantId(applicantId)
                .orElseThrow(() -> new AdminException("No hold request found for this applicant"));

        if (!otp.equals(hold.getOtp())) {
            throw new AdminException("Invalid verification code");
        }

        hold.setStatus(com.finflow.admin.entity.AdminHold.HoldStatus.ON_HOLD);
        hold.setAdminId(adminId);
        hold.setReason(remarks);
        hold.setOtp(null); // Clear OTP
        adminHoldRepository.save(hold);

        auditAction(adminId, null, "INTERNAL_HOLD", remarks + " (Applicant: " + applicantId + ")");
        
        // Block Login in Auth Service
        try {
            restTemplate.exchange(AUTH_INTERNAL_URL + "/" + applicantId + STATUS_QUERY_PARAM + "ON_HOLD", 
                HttpMethod.PATCH, getAdminEntity(adminEmail, adminId), Void.class);
            log.info("AUTH_SYNC: User {} status set to ON_HOLD in Auth Service", applicantId);
        } catch (Exception e) {
            log.warn("AUTH_SYNC: Failed to update auth status for {}: {}", applicantId, e.getMessage());
        }
        
        // Notify Applicant and Admin
        try {
            Map<String, Object> application = getApplicationDetailsForApplicant(applicantId);
            String applicantEmail = (String) application.get(FIELD_APPLICANT_USERNAME);
            String fullName = (String) application.get(FIELD_FULL_NAME);

            // 1. Notify Applicant
            com.finflow.notification.dto.NotificationRequest applicantNotify = com.finflow.notification.dto.NotificationRequest.builder()
                    .to(applicantEmail)
                    .subject("Urgent: Your FinFlow Account is on Hold")
                    .templateName("account-on-hold-template")
                    .model(java.util.Map.of("name", fullName != null ? fullName : "Valued Applicant"))
                    .build();
            rabbitTemplate.convertAndSend(notificationExchange, loanStatusRoutingKey, applicantNotify);

            // 2. Notify Admin
            if (adminEmail != null && !adminEmail.isBlank()) {
                com.finflow.notification.dto.NotificationRequest adminNotify = com.finflow.notification.dto.NotificationRequest.builder()
                        .to(adminEmail)
                        .subject("CRITICAL: Administrative Hold Applied - " + applicantId)
                        .templateName("admin-alert-template")
                        .model(java.util.Map.of(
                                FIELD_APPLICATION_ID, applicantId,
                                FIELD_STATUS, "ACCOUNT_ON_HOLD",
                                "timestamp", java.time.LocalDateTime.now().toString()
                        ))
                        .build();
                rabbitTemplate.convertAndSend(notificationExchange, loanStatusRoutingKey, adminNotify);
                log.info("Hold notifications sent to applicant {} and admin {}", applicantEmail, adminEmail);
            }
        } catch (Exception e) {
            log.error("Failed to send hold notifications for applicant {}: {}", applicantId, e.getMessage());
        }
        
        return Map.of(FIELD_MESSAGE, "Applicant placed on internal administrative hold", FIELD_STATUS, "ON_HOLD");
    }

    @Transactional
    @SuppressWarnings("null")
    public Object releaseHold(String applicantId, String adminId, String adminEmail) {
        com.finflow.admin.entity.AdminHold hold = adminHoldRepository.findByApplicantId(applicantId)
                .orElseThrow(() -> new AdminException("No hold found for this applicant"));

        hold.setStatus(com.finflow.admin.entity.AdminHold.HoldStatus.RELEASED);
        hold.setAdminId(adminId);
        adminHoldRepository.save(hold);

        auditAction(adminId, null, "INTERNAL_RELEASE", "Hold lifted for Applicant: " + applicantId);

        // Restore Login in Auth Service
        try {
            restTemplate.exchange(AUTH_INTERNAL_URL + "/" + applicantId + STATUS_QUERY_PARAM + "ACTIVE", 
                HttpMethod.PATCH, getAdminEntity(adminEmail, adminId), Void.class);
            log.info("AUTH_SYNC: User {} status restored to ACTIVE in Auth Service", applicantId);
        } catch (Exception e) {
            log.warn("AUTH_SYNC: Failed to restore auth status for {}: {}", applicantId, e.getMessage());
        }

        try {
            Map<String, Object> application = getApplicationDetailsForApplicant(applicantId);
            String email = (String) application.get(FIELD_APPLICANT_USERNAME);
            
            // Notify Applicant
            com.finflow.notification.dto.NotificationRequest applicantNotify = com.finflow.notification.dto.NotificationRequest.builder()
                    .to(email).subject("FinFlow: Account Restriction Lifted")
                    .templateName("loan-status-template")
                    .model(java.util.Map.of("name", application.get(FIELD_FULL_NAME), FIELD_APPLICATION_ID, "N/A", FIELD_STATUS, "ACTIVE"))
                    .build();
            rabbitTemplate.convertAndSend(notificationExchange, loanStatusRoutingKey, applicantNotify);

            // Notify Admin
            if (adminEmail != null && !adminEmail.isBlank()) {
                com.finflow.notification.dto.NotificationRequest adminNotify = com.finflow.notification.dto.NotificationRequest.builder()
                        .to(adminEmail).subject("Hold Released: " + applicantId)
                        .templateName("admin-alert-template")
                        .model(java.util.Map.of(FIELD_APPLICATION_ID, applicantId, FIELD_STATUS, "HOLD_RELEASED", "timestamp", java.time.LocalDateTime.now().toString()))
                        .build();
                rabbitTemplate.convertAndSend(notificationExchange, loanStatusRoutingKey, adminNotify);
            }
        } catch (Exception e) {
            log.warn("Failed to notify release: {}", e.getMessage());
        }

        return Map.of(FIELD_MESSAGE, "Administrative hold released", FIELD_STATUS, "RELEASED");
    }

    @CircuitBreaker(name = "authService")
    @SuppressWarnings("null")
    public Object getAllAdminUsers() {
        return restTemplate.exchange(AUTH_INTERNAL_URL + "/all", HttpMethod.GET, getAdminEntity(), Object.class)
                .getBody();
    }

    @CircuitBreaker(name = "authService")
    @SuppressWarnings("null")
    public Object getPendingAdmins() {
        return restTemplate.exchange(AUTH_INTERNAL_URL + "/pending", HttpMethod.GET, getAdminEntity(), Object.class)
                .getBody();
    }

    @CircuitBreaker(name = "authService")
    @SuppressWarnings("null")
    public Object approveAdminUser(String userId, String status) {
        String url = AUTH_INTERNAL_URL + "/" + userId + STATUS_QUERY_PARAM + status.toUpperCase();
        restTemplate.exchange(url, HttpMethod.PATCH, getAdminEntity(), Void.class);
        log.info("ADMIN_MGMT: Admin user {} decision applied: {}", userId, status);
        return Map.of("userId", userId, FIELD_STATUS, status.toUpperCase(), FIELD_MESSAGE, "Admin status updated successfully");
    }

    @CircuitBreaker(name = "authService")
    @SuppressWarnings("null")
    public Object getAllUsers() {
        return restTemplate.exchange(AUTH_INTERNAL_URL + "/everybody", HttpMethod.GET, getAdminEntity(), Object.class)
                .getBody();
    }
}
