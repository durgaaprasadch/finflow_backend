package com.finflow.application.controller;

import com.finflow.application.dto.*;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicantApplicationController {

    private static final String ROLE_APPLICANT = "APPLICANT";
    private static final String HEADER_USER_ROLE = "userRole";
    private static final String HEADER_LOGGED_IN_USER = "loggedInUser";
    private static final String HEADER_APPLICANT_ID = "applicantId";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final String FIELD_LOAN_TYPE = "loanType";
    private static final String FIELD_REQUESTED_AMOUNT = "requestedAmount";
    private static final String FIELD_APPLICATION_ID = "applicationId";

    private final LoanApplicationService service;

    public ApplicantApplicationController(LoanApplicationService service) {
        this.service = service;
    }

    @Tag(name = "Loan Applicant Journey")
    @Operation(summary = "Step 1: Create application draft")
    @PostMapping
    public ApiResponse<Map<String, Object>> createApplication(
            @RequestParam(FIELD_LOAN_TYPE) com.finflow.application.entity.LoanType loanType,
            @RequestParam(FIELD_REQUESTED_AMOUNT) java.math.BigDecimal requestedAmount,
            HttpServletRequest request) {
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String applicantUsername = request.getHeader(HEADER_LOGGED_IN_USER);
        String applicantId = request.getHeader(HEADER_APPLICANT_ID);
        ensureApplicantRole(role);
        
        CreateApplicationRequest createRequest = new CreateApplicationRequest();
        createRequest.setLoanType(loanType);
        createRequest.setRequestedAmount(requestedAmount);
        
        LoanApplication app = service.createApplicantDraft(createRequest, applicantId, applicantUsername);
        return ApiResponse.success("Application created", toSummary(app));
    }

    @Tag(name = "Loan Applicant Journey")
    @Operation(summary = "Step 2: Update personal details", description = "Automatically updates your current active draft.")
    @PatchMapping("/personal")
    public ApiResponse<Map<String, Object>> updatePersonalDetails(
            @jakarta.validation.Valid @RequestBody PersonalDetailsRequest detailsRequest,
            HttpServletRequest request) {
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String username = request.getHeader(HEADER_LOGGED_IN_USER);
        String id = request.getHeader(HEADER_APPLICANT_ID);
        ensureApplicantRole(role);
        
        LoanApplication app = service.updatePersonalDetails(id, username, detailsRequest);
        Map<String, Object> data = toSummary(app);
        data.put("personalDetails", personalDetails(app));
        return ApiResponse.success("Personal details updated successfully", data);
    }

    @Tag(name = "Loan Applicant Journey")
    @Operation(summary = "Step 3: Update employment details")
    @PatchMapping("/employment")
    public ApiResponse<Map<String, Object>> updateEmploymentDetails(
            @RequestBody EmploymentDetailsRequest employmentRequest,
            HttpServletRequest request) {
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String username = request.getHeader(HEADER_LOGGED_IN_USER);
        String id = request.getHeader(HEADER_APPLICANT_ID);
        ensureApplicantRole(role);
        
        LoanApplication app = service.updateEmploymentDetails(id, username, employmentRequest);
        Map<String, Object> data = toSummary(app);
        data.put("employmentDetails", employmentDetails(app));
        return ApiResponse.success("Employment details updated successfully", data);
    }

    @Tag(name = "Loan Applicant Journey")
    @Operation(summary = "Step 4: Update loan details")
    @PatchMapping("/loan")
    public ApiResponse<Map<String, Object>> updateLoanDetails(
            @RequestParam java.math.BigDecimal loanAmount,
            @RequestParam Integer tenureMonths,
            @RequestParam com.finflow.application.entity.LoanType loanType,
            HttpServletRequest request) {
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String username = request.getHeader(HEADER_LOGGED_IN_USER);
        String id = request.getHeader(HEADER_APPLICANT_ID);
        ensureApplicantRole(role);
        
        LoanDetailsRequest loanRequest = new LoanDetailsRequest();
        loanRequest.setLoanAmount(loanAmount);
        loanRequest.setTenureMonths(tenureMonths);
        loanRequest.setLoanType(loanType);
        
        LoanApplication app = service.updateLoanDetails(id, username, loanRequest);
        Map<String, Object> data = toSummary(app);
        data.put("loanDetails", loanDetails(app));
        return ApiResponse.success("Loan details updated successfully", data);
    }

    @Tag(name = "Loan Applicant Journey")
    @Operation(summary = "Step 5: Submit application (Final)")
    @PatchMapping("/submit")
    public ApiResponse<Map<String, Object>> submitApplication(HttpServletRequest request) {
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String username = request.getHeader(HEADER_LOGGED_IN_USER);
        String id = request.getHeader(HEADER_APPLICANT_ID);
        ensureApplicantRole(role);
        
        LoanApplication app = service.submitApplicantApplication(id, username);
        Map<String, Object> data = toSummary(app);
        data.put("submittedAt", app.getSubmittedAt());
        return ApiResponse.success("Application submitted successfully", data);
    }

    @Tag(name = "Loan Applicant Journey")
    @Operation(summary = "Step 6: Track current status")
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getApplicationStatus(HttpServletRequest request) {
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String username = request.getHeader(HEADER_LOGGED_IN_USER);
        String id = request.getHeader(HEADER_APPLICANT_ID);
        ensureApplicantRole(role);
        
        LoanApplication app = service.findActiveApplicationByApplicant(id, username);
        Map<String, Object> data = toSummary(app);
        data.put("timeline", service.getApplicantTimeline(app.getId(), id, username));
        return ApiResponse.success("Current application status fetched", data);
    }

    @Tag(name = "Loan Applicant Journey")
    @Operation(summary = "Step 7: View history")
    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> getMyApplications(HttpServletRequest request) {
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String username = request.getHeader(HEADER_LOGGED_IN_USER);
        String id = request.getHeader(HEADER_APPLICANT_ID);
        ensureApplicantRole(role);
        
        List<Map<String, Object>> data = service.getApplicationsByApplicant(id, username).stream()
                .map(this::toSummary)
                .toList();
        return ApiResponse.success("Application history fetched", data);
    }

    @Tag(name = "Loan Applicant Journey")
    @Operation(summary = "Action: Delete active draft", description = "Permanently remove your current draft to start fresh.")
    @DeleteMapping("/draft")
    public ApiResponse<String> deleteDraft(HttpServletRequest request) {
        String role = getHeader(request, HEADER_USER_ROLE, ROLE_APPLICANT);
        String username = request.getHeader(HEADER_LOGGED_IN_USER);
        String id = request.getHeader(HEADER_APPLICANT_ID);
        ensureApplicantRole(role);
        
        service.deleteDraftByApplicant(id, username);
        return ApiResponse.success("Application draft deleted successfully", "DELETED");
    }

    private void ensureApplicantRole(String role) {
        if (!ROLE_APPLICANT.equalsIgnoreCase(role)) {
            throw new com.finflow.application.exception.ApplicationException("Unauthorized: Only APPLICANT can access these endpoints");
        }
    }

    private String getHeader(HttpServletRequest request, String name, String defaultValue) {
        String val = request.getHeader(name);
        return val != null ? val : defaultValue;
    }

    private Map<String, Object> toSummary(LoanApplication app) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put(FIELD_APPLICATION_ID, app.getId());
        summary.put(FIELD_LOAN_TYPE, app.getLoanType());
        summary.put(FIELD_REQUESTED_AMOUNT, app.getAmount());
        summary.put(FIELD_STATUS, app.getStatus());
        summary.put(FIELD_UPDATED_AT, app.getUpdatedAt());
        return summary;
    }

    private Map<String, Object> personalDetails(LoanApplication app) {
        Map<String, Object> personal = new LinkedHashMap<>();
        personal.put("fullName", app.getFullName());
        personal.put("pan", app.getPanNumber());
        personal.put("aadhaar", app.getAadhaarNumber());
        return personal;
    }

    private Map<String, Object> employmentDetails(LoanApplication app) {
        Map<String, Object> employment = new LinkedHashMap<>();
        employment.put("company", app.getCompanyName());
        employment.put("income", app.getMonthlyIncome());
        return employment;
    }

    private Map<String, Object> loanDetails(LoanApplication app) {
        Map<String, Object> loan = new LinkedHashMap<>();
        loan.put("amount", app.getLoanAmount());
        loan.put("tenure", app.getTenure());
        loan.put("purpose", app.getPurpose());
        return loan;
    }
}
