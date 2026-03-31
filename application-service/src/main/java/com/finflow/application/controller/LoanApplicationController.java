package com.finflow.application.controller;

import com.finflow.application.dto.LoanApplicationResponse;
import com.finflow.application.entity.LoanApplication;
import com.finflow.application.exception.ApplicationException;
import com.finflow.application.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@Hidden
public class LoanApplicationController {

    private static final String ROLE_ADMIN = "ADMIN";
    private final LoanApplicationService service;

    public LoanApplicationController(LoanApplicationService service) {
        this.service = service;
    }

    // ✅ CREATE RESOURCE
    @io.swagger.v3.oas.annotations.Operation(tags = {"Start New Loan"})
    @PostMapping
    public LoanApplicationResponse create(
            @RequestBody com.finflow.application.dto.CreateApplicationRequest req,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader("loggedInUser") String username
    ) {
        LoanApplication application = new LoanApplication();
        application.setLoanType(req.getLoanType() != null ? req.getLoanType().name() : null);
        application.setAmount(req.getRequestedAmount());
        application.setTenure(req.getTenureMonths());
        application.setPurpose(req.getPurpose());
        application.setApplicantUsername(username);
        return LoanApplicationResponse.fromEntity(service.createDraft(application));
    }

    // ✅ LIST MY RESOURCES
    @io.swagger.v3.oas.annotations.Operation(tags = {"Track My Applications"})
    @GetMapping("/me")
    public List<LoanApplicationResponse> getMyApplications(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = "loggedInUser", required = false) String loggedInUser) {
        
        if (loggedInUser == null || loggedInUser.isEmpty()) {
            throw new ApplicationException("User ID not found in security context");
        }
        return service.getApplicationsByUser(loggedInUser).stream()
                .map(LoanApplicationResponse::fromEntity)
                .toList();
    }

    // ✅ GET BY ID (INTERNAL ADMIN ONLY - HIDDEN FROM SWAGGER)
    @io.swagger.v3.oas.annotations.Hidden
    @GetMapping("/{id}")
    public LoanApplicationResponse getApplication(
            @PathVariable("id") Long id,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = "userRole", defaultValue = "APPLICANT") String role) {
        
        if (!ROLE_ADMIN.equalsIgnoreCase(role)) {
            throw new ApplicationException("Unauthorized: Only ADMIN can view specific applications by ID");
        }
        return LoanApplicationResponse.fromEntity(service.getApplicationById(id));
    }

    // ✅ GET ALL (INTERNAL PURPOSES ONLY - HIDDEN FROM SWAGGER)
    @io.swagger.v3.oas.annotations.Hidden
    @GetMapping("/all")
    public List<LoanApplicationResponse> getAllApplications(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = "userRole", defaultValue = "APPLICANT") String role) {
        
        if (!ROLE_ADMIN.equalsIgnoreCase(role)) {
            throw new ApplicationException("Unauthorized: Only ADMIN can view all applications");
        }
        return service.getAllApplications().stream()
                .map(LoanApplicationResponse::fromEntity)
                .toList();
    }

    @io.swagger.v3.oas.annotations.Hidden
    @GetMapping("/applicant/{applicantId}")
    public LoanApplicationResponse getByApplicant(
            @PathVariable("applicantId") String applicantId,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = "userRole", defaultValue = "APPLICANT") String role) {
        
        if (!ROLE_ADMIN.equalsIgnoreCase(role)) {
            throw new ApplicationException("Unauthorized: Only ADMIN can view applications by applicant ID");
        }
        return LoanApplicationResponse.fromEntity(service.findActiveApplicationByApplicant(applicantId, applicantId));
    }

    // ✅ PATCH STATUS
    @io.swagger.v3.oas.annotations.Operation(tags = {"Manage Application Status"})
    @PatchMapping("/{id}/status")
    public LoanApplicationResponse updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") com.finflow.application.entity.LoanStatus status,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = "userRole", defaultValue = "APPLICANT") String role,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @RequestHeader(value = "loggedInUser", required = false) String loggedInUser) {

        return LoanApplicationResponse.fromEntity(service.updateStatus(id, status, role, loggedInUser));
    }


}
