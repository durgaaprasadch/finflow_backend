package com.finflow.application.service;

import com.finflow.application.dto.CreateApplicationRequest;
import com.finflow.application.dto.EmploymentDetailsRequest;
import com.finflow.application.dto.LoanDetailsRequest;
import com.finflow.application.dto.PersonalDetailsRequest;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.entity.LoanStatus;
import com.finflow.application.exception.ApplicationException;
import com.finflow.application.exception.InvalidTransitionException;
import com.finflow.application.entity.LoanStatusHistory;
import com.finflow.application.messaging.ApplicationEventPublisher;
import com.finflow.application.repository.LoanApplicationRepository;
import com.finflow.application.repository.LoanStatusHistoryRepository;
import com.finflow.application.entity.DocumentRequirement;
import com.finflow.application.repository.DocumentRequirementRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@SuppressWarnings("null")
public class LoanApplicationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoanApplicationService.class);
    private static final String UNAUTHORIZED_MSG = "Unauthorized: Only ADMIN can set status to ";
    private static final Set<String> ALLOWED_EMPLOYMENT_TYPES = Set.of(
            "SALARIED", "SELF_EMPLOYED", "BUSINESS", "FREELANCER");
    private static final Set<String> DEFAULT_REQUIRED_DOCUMENT_TYPES = Set.of(
            "AADHAAR", "PAN", "SALARY_SLIP", "BANK_STATEMENT", "PHOTO");
    private static final String ROLE_ADMIN_LITERAL = "ADMIN";
    private static final String HEADER_USER_ROLE = "userRole";

    private final LoanApplicationRepository repository;
    private final LoanStatusHistoryRepository historyRepository;
    private final DocumentRequirementRepository requirementRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RestTemplate restTemplate;

    public LoanApplicationService(LoanApplicationRepository repository, LoanStatusHistoryRepository historyRepository,
            DocumentRequirementRepository requirementRepository, ApplicationEventPublisher eventPublisher,
            RestTemplate restTemplate) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.requirementRepository = requirementRepository;
        this.eventPublisher = eventPublisher;
        this.restTemplate = restTemplate;
    }

    private static final String DOCUMENT_SERVICE_URL = "http://document-service/api/documents";

    public LoanApplication createDraft(LoanApplication app) {
        app.setStatus(LoanStatus.DRAFT);
        return repository.save(app);
    }

    public LoanApplication createApplicantDraft(CreateApplicationRequest request, String applicantId,
            String applicantUsername) {
        if (request == null) {
            throw new ApplicationException("Application request is required");
        }
        if (applicantId == null || applicantId.isBlank()) {
            throw new ApplicationException("Applicant ID not found in security context");
        }
        if (applicantUsername == null || applicantUsername.isBlank()) {
            throw new ApplicationException("Applicant username not found in security context");
        }

        // ONE-ACTIVE-APPLICATION RULE: Check for any existing application that isn't final (not Approved/Rejected/Closed)
        List<LoanApplication> userApps = repository.findByApplicantUsername(applicantUsername);
        for (LoanApplication existing : userApps) {
            LoanStatus s = existing.getStatus();
            if (s != LoanStatus.APPROVED && s != LoanStatus.REJECTED && s != LoanStatus.CLOSED) {
                throw new ApplicationException("You already have an active application in progress (ID: "
                        + existing.getId() + ", Status: " + s + "). You can only start a new application after your current one is APPROVED or REJECTED.");
            }
        }

        if (request.getLoanType() == null) {
            throw new ApplicationException("loanType is required");
        }
        if (request.getRequestedAmount() == null || request.getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApplicationException("requestedAmount must be greater than 0");
        }

        LoanApplication app = new LoanApplication();
        app.setApplicantId(applicantId);
        app.setApplicantUsername(applicantUsername);

        // Use provided loanType if given, otherwise base it on purpose if available
        if (request.getLoanType() != null) {
            app.setLoanType(request.getLoanType().name());
        } else if (request.getPurpose() != null) {
            app.setLoanType(normalizeUpper(request.getPurpose()) + "_LOAN");
        }

        app.setAmount(request.getRequestedAmount());
        app.setLoanAmount(request.getRequestedAmount());
        app.setTenure(request.getTenureMonths());
        app.setPurpose(normalizeUpper(request.getPurpose()));

        app.setStatus(LoanStatus.DRAFT);
        return repository.save(app);
    }

    public void deleteDraftByApplicant(String applicantId, String applicantUsername) {
        LoanApplication app = findActiveApplicationByApplicant(applicantId, applicantUsername);
        Long applicationId = app.getId();

        // Safety check: Only applications NOT yet submitted can be deleted by the user
        if (app.getStatus() == LoanStatus.SUBMITTED ||
                app.getStatus() == LoanStatus.DOCS_VERIFIED ||
                app.getStatus() == LoanStatus.VERIFIED ||
                app.getStatus() == LoanStatus.APPROVED ||
                app.getStatus() == LoanStatus.REJECTED ||
                app.getStatus() == LoanStatus.CLOSED) {
            throw new ApplicationException("Applications cannot be deleted after submission or final decision.");
        }

        // Remove history then application
        historyRepository.deleteByApplicationId(applicationId);
        repository.delete(app);
        log.info("USER_ACTION: Applicant {} deleted their unsubmitted application {}", applicantUsername,
                applicationId);
    }

    public LoanApplication getApplicationById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }

    public List<LoanApplication> getApplicationsByUser(String username) {
        return repository.findByApplicantUsername(username);
    }

    public List<LoanApplication> getApplicationsByApplicant(String applicantId, String username) {
        if (applicantId != null && !applicantId.isBlank()) {
            return repository.findByApplicantIdOrderByCreatedAtDesc(applicantId);
        }
        List<LoanApplication> applications = repository.findByApplicantUsername(username);
        applications.sort(
                Comparator.comparing(LoanApplication::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return applications;
    }

    public List<LoanApplication> getAllApplications() {
        return repository.findAll();
    }

    public LoanApplication findActiveApplicationByApplicant(String applicantId, String applicantUsername) {
        // Find the latest application that isn't CLOSED, prioritize applicantId
        List<LoanApplication> apps;
        if (applicantId != null && !applicantId.isBlank()) {
            apps = repository.findByApplicantIdOrderByCreatedAtDesc(applicantId);
        } else {
            apps = repository.findByApplicantUsername(applicantUsername);
        }

        return apps.stream()
                .filter(app -> app.getStatus() != LoanStatus.CLOSED)
                .max(Comparator.comparing(LoanApplication::getCreatedAt))
                .orElseThrow(() -> new ApplicationException(
                        "No active loan application found for user: " + (applicantId != null ? applicantId : applicantUsername)));
    }

    public LoanApplication updatePersonalDetails(String applicantId, String applicantUsername,
            PersonalDetailsRequest request) {
        LoanApplication app = findActiveApplicationByApplicant(applicantId, applicantUsername);
        requireStatus(app, LoanStatus.DRAFT, "Personal details can only be updated while the application is in DRAFT.");

        if (request == null || isBlank(request.getFullName()) || request.getDob() == null
                || isBlank(request.getGender()) || isBlank(request.getMaritalStatus())
                || isBlank(request.getPanNumber()) || isBlank(request.getAadhaarNumber())
                || request.getAddress() == null || isBlank(request.getAddress().getLine1())
                || isBlank(request.getAddress().getCity()) || isBlank(request.getAddress().getState())
                || isBlank(request.getAddress().getPincode())) {
            throw new ApplicationException("All personal details are required");
        }

        LoanStatus previousStatus = app.getStatus();
        app.setFullName(request.getFullName());
        app.setDob(request.getDob());
        app.setGender(normalizeUpper(request.getGender()));
        app.setMaritalStatus(normalizeUpper(request.getMaritalStatus()));
        app.setPanNumber(request.getPanNumber());
        app.setAadhaarNumber(request.getAadhaarNumber());
        app.setAddressLine1(request.getAddress().getLine1());
        app.setCity(request.getAddress().getCity());
        app.setState(request.getAddress().getState());
        app.setPincode(request.getAddress().getPincode());
        app.setStatus(LoanStatus.DETAILS_UPLOADED);
        logStatusChange(app.getId(), previousStatus, LoanStatus.DETAILS_UPLOADED, applicantUsername,
                "Personal details updated");
        return repository.save(app);
    }

    public LoanApplication updateEmploymentDetails(String applicantId, String applicantUsername,
            EmploymentDetailsRequest request) {
        LoanApplication app = findActiveApplicationByApplicant(applicantId, applicantUsername);
        requireStatus(app, LoanStatus.DETAILS_UPLOADED,
                "Employment details can only be updated after personal details are uploaded.");

        if (request == null) {
            throw new ApplicationException("Employment details request is required");
        }

        String employmentType = normalizeUpper(request.getEmploymentType());
        if (!ALLOWED_EMPLOYMENT_TYPES.contains(employmentType) || isBlank(request.getCompanyName())
                || request.getMonthlyIncome() == null || request.getMonthlyIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApplicationException("Valid employmentType, companyName, and monthlyIncome are required");
        }

        LoanStatus previousStatus = app.getStatus();
        app.setEmploymentType(employmentType);
        app.setCompanyName(request.getCompanyName());
        app.setDesignation(request.getDesignation());
        app.setMonthlyIncome(request.getMonthlyIncome());
        app.setExperienceYears(request.getExperienceYears());
        app.setStatus(LoanStatus.EMPLOYMENT_ADDED);
        logStatusChange(app.getId(), previousStatus, LoanStatus.EMPLOYMENT_ADDED, applicantUsername,
                "Employment details updated");
        return repository.save(app);
    }

    public LoanApplication updateLoanDetails(String applicantId, String applicantUsername,
            LoanDetailsRequest request) {
        LoanApplication app = findActiveApplicationByApplicant(applicantId, applicantUsername);
        requireStatus(app, LoanStatus.EMPLOYMENT_ADDED,
                "Loan details can only be updated after employment details are added.");

        if (request == null || request.getLoanAmount() == null
                || request.getLoanAmount().compareTo(BigDecimal.ZERO) <= 0
                || request.getTenureMonths() == null || request.getTenureMonths() <= 0
                || request.getLoanType() == null) {
            throw new ApplicationException("Valid loanAmount, tenureMonths, and loanType are required");
        }

        LoanStatus previousStatus = app.getStatus();
        app.setLoanAmount(request.getLoanAmount());
        app.setTenure(request.getTenureMonths());
        app.setLoanType(request.getLoanType().name());
        app.setPurpose(request.getLoanType().name()); // Populate purpose from loanType for internal continuity

        app.setStatus(LoanStatus.LOAN_DETAILS_ADDED);
        logStatusChange(app.getId(), previousStatus, LoanStatus.LOAN_DETAILS_ADDED, applicantUsername,
                "Loan details updated");
        return repository.save(app);
    }

    public LoanApplication submitApplicantApplication(String applicantId, String applicantUsername) {
        LoanApplication app = findActiveApplicationByApplicant(applicantId, applicantUsername);
        Long id = app.getId();
        LoanStatus currentStatus = app.getStatus();

        // If not in DOCUMENTS_COMPLETED, try to sync status once before failing
        if (currentStatus != LoanStatus.DOCUMENTS_COMPLETED) {
            log.info("Application {} status is {}. Performing dynamic document check before submission.", id,
                    currentStatus);
            if (checkRequirementsMet(app)) {
                log.info("Requirements met for App {}. Updating status to DOCUMENTS_COMPLETED.", id);
                app.setStatus(LoanStatus.DOCUMENTS_COMPLETED);
                app = repository.save(app);
                currentStatus = LoanStatus.DOCUMENTS_COMPLETED;
            }
        }

        if (currentStatus != LoanStatus.DOCUMENTS_COMPLETED) {
            throw new ApplicationException(
                    "Application can be submitted only after all required documents are uploaded.");
        }

        LoanStatus previousStatus = app.getStatus();
        app.setStatus(LoanStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());
        LoanApplication saved = repository.save(app);
        logStatusChange(id, previousStatus, LoanStatus.SUBMITTED, applicantUsername,
                "Application submitted successfully");
        eventPublisher.publishApplicationSubmittedEvent(saved);
        return saved;
    }

    public LoanApplication getApplicantApplicationById(Long id, String applicantId, String applicantUsername) {
        return getApplicantOwnedApplication(id, applicantId, applicantUsername);
    }

    public List<Map<String, Object>> getApplicantTimeline(Long id, String applicantId, String applicantUsername) {
        LoanApplication app = getApplicantOwnedApplication(id, applicantId, applicantUsername);
        List<LoanStatusHistory> history = historyRepository.findByApplicationId(app.getId());
        List<Map<String, Object>> timeline = new ArrayList<>();

        if (app.getCreatedAt() != null) {
            timeline.add(buildTimelineEntry(LoanStatus.DRAFT.name(), app.getCreatedAt()));
        }
        history.stream()
                .sorted(Comparator.comparing(LoanStatusHistory::getChangedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(entry -> timeline.add(buildTimelineEntry(entry.getToStatus().name(), entry.getChangedAt())));
        return timeline;
    }

    public LoanApplication updateStatus(Long id, LoanStatus newStatus, String role, String loggedInUser) {
        LoanApplication app = getApplicationById(id);
        LoanStatus currentStatus = app.getStatus();

        if (currentStatus == LoanStatus.CLOSED) {
            throw new InvalidTransitionException(
                    "Unauthorized: Application is CLOSED. It cannot be reopened or modified by anyone.");
        }

        // 1. RBAC Enforcement: Only ADMIN can do everything. Regular users can only
        // submit.
        if (!ROLE_ADMIN_LITERAL.equalsIgnoreCase(role) && newStatus != LoanStatus.SUBMITTED) {
            throw new ApplicationException(
                    UNAUTHORIZED_MSG + newStatus + ". Regular users can only submit applications.");
        }

        if (!ROLE_ADMIN_LITERAL.equalsIgnoreCase(role)) {
            if (loggedInUser == null || loggedInUser.isBlank()) {
                throw new ApplicationException("Unauthorized: Missing authenticated user");
            }
            if (app.getApplicantUsername() == null || !app.getApplicantUsername().equalsIgnoreCase(loggedInUser)) {
                throw new ApplicationException("Unauthorized: Applicants can only modify their own applications.");
            }
        }

        // 2. State Machine Enforcement
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new InvalidTransitionException(
                    "Strict transitions policy violated. Cannot transition from " + currentStatus + " to " + newStatus);
        }

        app.setStatus(newStatus);
        LoanApplication savedApp = repository.save(app);

        // Audit Trail: Log the status change
        logStatusChange(id, currentStatus, newStatus, role, "Status updated by user/admin");

        // Event-Driven: Trigger RabbitMQ event when application is finally submitted
        if (LoanStatus.SUBMITTED.equals(newStatus)) {
            eventPublisher.publishApplicationSubmittedEvent(savedApp);
        } else if (newStatus != null) {
            // Send email notification for other status changes (APPROVED, REJECTED, REVIEW,
            // etc.)
            eventPublisher.publishStatusUpdateNotification(savedApp);
        }

        return savedApp;
    }

    public void syncStatusAfterDocumentUpload(Long applicationId) {
        LoanApplication app = getApplicationById(applicationId);
        LoanStatus current = app.getStatus();

        if (current == LoanStatus.LOAN_DETAILS_ADDED || current == LoanStatus.DETAILS_UPLOADED
                || current == LoanStatus.EMPLOYMENT_ADDED) {
            boolean allPresent = checkRequirementsMet(app);

            LoanStatus nextStatus = allPresent ? LoanStatus.DOCUMENTS_COMPLETED : current;

            if (nextStatus != current) {
                app.setStatus(nextStatus);
                repository.save(app);
                logStatusChange(app.getId(), current, nextStatus, "SYSTEM",
                        "Automated sync after document upload. Requirements met: " + allPresent);
                log.info("Application {} synced to {} due to document upload.", applicationId, nextStatus);
            }
            return;
        }

        // If the legacy flow is waiting for docs, move it
        if (current == LoanStatus.SUBMITTED || current == LoanStatus.DOCS_PENDING || current == LoanStatus.PARTIAL) {

            boolean allPresent = checkRequirementsMet(app);

            LoanStatus nextStatus = allPresent ? LoanStatus.UPLOADED : LoanStatus.PARTIAL;

            if (nextStatus != current) {
                app.setStatus(nextStatus);
                repository.save(app);
                logStatusChange(app.getId(), current, nextStatus, "SYSTEM",
                        "Automated sync after document upload. Requirements met: " + allPresent);
                log.info("Application {} synced to {} due to document upload.", applicationId, nextStatus);
            }
        }
    }

    private boolean checkRequirementsMet(LoanApplication application) {
        try {
            if (application.getLoanType() == null || application.getLoanType().isBlank()) {
                log.warn("Skipping completion for App {} because loanType is missing.", application.getId());
                return false;
            }

            List<DocumentRequirement> requirements = requirementRepository
                    .findByLoanTypeIgnoreCaseAndMandatoryTrue(application.getLoanType());
            Set<String> requiredDocumentTypes = requirements.isEmpty()
                    ? new HashSet<>(DEFAULT_REQUIRED_DOCUMENT_TYPES)
                    : requirements.stream()
                            .map(DocumentRequirement::getDocumentType)
                            .filter(type -> type != null && !type.isBlank())
                            .map(String::toUpperCase)
                            .collect(java.util.stream.Collectors.toSet());

            // Call document-service to see what's uploaded
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set(HEADER_USER_ROLE, ROLE_ADMIN_LITERAL); // System internal call
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            List<?> uploadedDocs = restTemplate.exchange(DOCUMENT_SERVICE_URL + "/application/" + application.getId(),
                    org.springframework.http.HttpMethod.GET, entity, List.class).getBody();

            if (uploadedDocs == null || uploadedDocs.isEmpty())
                return false;

            Set<String> uploadedDocumentTypes = extractUploadedDocumentTypes(uploadedDocs);
            return requiredDocumentTypes.stream().allMatch(uploadedDocumentTypes::contains);

        } catch (Exception e) {
            log.error("Error checking document requirements for App {}: {}", application.getId(), e.getMessage());
            return false;
        }
    }

    private Set<String> extractUploadedDocumentTypes(List<?> uploadedDocs) {
        Set<String> documentTypes = new HashSet<>();
        for (Object uploadedDoc : uploadedDocs) {
            if (uploadedDoc instanceof Map<?, ?> map) {
                Object documentType = map.get("documentType");
                if (documentType != null) {
                    documentTypes.add(documentType.toString().toUpperCase());
                }
            } else if (uploadedDoc instanceof String documentType) {
                documentTypes.add(documentType.toUpperCase());
            }
        }
        return documentTypes;
    }

    private void logStatusChange(Long applicationId, LoanStatus oldStatus, LoanStatus newStatus, String changedBy,
            String comments) {
        LoanStatusHistory history = new LoanStatusHistory();
        history.setApplicationId(applicationId);
        history.setFromStatus(oldStatus);
        history.setToStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setChangedAt(java.time.LocalDateTime.now());
        history.setReason(comments);
        historyRepository.save(history);
    }

    private boolean isValidTransition(LoanStatus current, LoanStatus next) {
        return switch (current) {
            case DRAFT -> next == LoanStatus.SUBMITTED || next == LoanStatus.DETAILS_UPLOADED;
            case DETAILS_UPLOADED -> next == LoanStatus.EMPLOYMENT_ADDED;
            case EMPLOYMENT_ADDED -> next == LoanStatus.LOAN_DETAILS_ADDED;
            case LOAN_DETAILS_ADDED -> next == LoanStatus.DOCUMENTS_COMPLETED;
            case DOCUMENTS_COMPLETED -> next == LoanStatus.SUBMITTED;
            case SUBMITTED -> next == LoanStatus.DOCS_VERIFIED || next == LoanStatus.DOCS_PENDING
                    || next == LoanStatus.UPLOADED || next == LoanStatus.PARTIAL || next == LoanStatus.REVIEW;
            case DOCS_VERIFIED ->
                next == LoanStatus.APPROVED || next == LoanStatus.REJECTED || next == LoanStatus.REUPLOAD;
            case DOCS_PENDING -> next == LoanStatus.UPLOADED || next == LoanStatus.PARTIAL
                    || next == LoanStatus.REUPLOAD || next == LoanStatus.REJECTED;
            case PARTIAL -> next == LoanStatus.UPLOADED || next == LoanStatus.REUPLOAD;
            case UPLOADED -> next == LoanStatus.REVIEW || next == LoanStatus.REUPLOAD || next == LoanStatus.FAIL
                    || next == LoanStatus.DOCS_VERIFIED;
            case REVIEW ->
                next == LoanStatus.VERIFIED || next == LoanStatus.DOCS_VERIFIED || next == LoanStatus.REUPLOAD
                        || next == LoanStatus.FAIL || next == LoanStatus.REJECTED || next == LoanStatus.DOCS_PENDING;
            case VERIFIED -> next == LoanStatus.APPROVED || next == LoanStatus.REJECTED || next == LoanStatus.REUPLOAD;
            case REUPLOAD -> next == LoanStatus.DOCS_PENDING || next == LoanStatus.PARTIAL;
            case FAIL -> next == LoanStatus.REUPLOAD || next == LoanStatus.REJECTED;
            case APPROVED, REJECTED -> next == LoanStatus.CLOSED;
            case CLOSED -> false;
        };
    }

    private LoanApplication getApplicantOwnedApplication(Long id, String applicantId, String applicantUsername) {
        LoanApplication app = getApplicationById(id);
        if (applicantId != null && !applicantId.isBlank() && app.getApplicantId() != null
                && !app.getApplicantId().equals(applicantId)) {
            throw new ApplicationException("Unauthorized: Application does not belong to the authenticated applicant.");
        }
        if (app.getApplicantUsername() == null || applicantUsername == null
                || !app.getApplicantUsername().equalsIgnoreCase(applicantUsername)) {
            throw new ApplicationException("Unauthorized: Application does not belong to the authenticated applicant.");
        }
        return app;
    }

    private void requireStatus(LoanApplication app, LoanStatus expected, String message) {
        if (app.getStatus() != expected) {
            throw new ApplicationException(message);
        }
    }

    private Map<String, Object> buildTimelineEntry(String status, LocalDateTime time) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("status", status);
        entry.put("time", time);
        return entry;
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
