package com.finflow.admin.controller;

import com.finflow.admin.dto.ApiResponse;
import com.finflow.admin.service.AdminService;
import com.finflow.admin.exception.AdminException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminWorkflowController {

    private static final String LOGGED_IN_USER = "loggedInUser";
    private static final String APPLICANT_ID_HEADER = "applicantId";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final AdminService adminService;

    public AdminWorkflowController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Tag(name = "INTERNAL: Administrative User Controls", description = "Endpoints for admin-service to locally manage user states (Internal Audit)")
    @Operation(summary = "Request OTP to put applicant on hold")
    @PostMapping("/applicants/{applicantId}/hold-request")
    public ApiResponse<Object> requestHold(@PathVariable String applicantId) {
        return ApiResponse.success("Hold request initiated locally", adminService.requestHold(applicantId));
    }

    @Tag(name = "INTERNAL: Administrative User Controls", description = "Endpoints for admin-service to locally manage user states (Internal Audit)")
    @Operation(summary = "Verify OTP and set applicant to ON_HOLD (Internal)")
    @PatchMapping("/applicants/{applicantId}/hold-verify")
    public ApiResponse<Object> verifyHold(
            @PathVariable String applicantId,
            @RequestParam String otp,
            @RequestParam String remarks,
            @Parameter(hidden = true) HttpServletRequest request) {
        String adminId = request.getHeader(APPLICANT_ID_HEADER);
        String adminEmail = request.getHeader(LOGGED_IN_USER);
        return ApiResponse.success("Hold verified locally", adminService.verifyHold(applicantId, otp, adminId, adminEmail, remarks));
    }

    @Tag(name = "INTERNAL: Administrative User Controls", description = "Endpoints for admin-service to locally manage user states (Internal Audit)")
    @Operation(summary = "Release administrative hold on applicant")
    @PatchMapping("/applicants/{applicantId}/hold-release")
    public ApiResponse<Object> releaseHold(
            @PathVariable String applicantId,
            @Parameter(hidden = true) HttpServletRequest request) {
        String adminId = request.getHeader(APPLICANT_ID_HEADER);
        String adminEmail = request.getHeader(LOGGED_IN_USER);
        return ApiResponse.success("Hold released locally", adminService.releaseHold(applicantId, adminId, adminEmail));
    }

    @Tag(name = "User Administration")
    @Operation(summary = "Approve or reject pending admin user")
    @PatchMapping("/users/decision/{userId}")
    public ApiResponse<Object> approveAdmin(@PathVariable String userId, @RequestParam String status) {
        return ApiResponse.success("Admin decision (stub)", adminService.approveAdminUser(userId, status));
    }

    @Tag(name = "User Administration")
    @Operation(summary = "Get all admin users")
    @GetMapping("/users")
    public ApiResponse<Object> getAdmins() {
        return ApiResponse.success("Admin fetch (stub)", adminService.getAllAdminUsers());
    }

    @Tag(name = "User Administration")
    @Operation(summary = "Get admin users waiting for approval")
    @GetMapping("/users/pending")
    public ApiResponse<Object> getPendingAdmins() {
        return ApiResponse.success("Pending fetch (stub)", adminService.getPendingAdmins());
    }

    @Tag(name = "User Administration")
    @Operation(summary = "Get all users (Applicants and Admins)")
    @GetMapping("/users/all-users")
    public ApiResponse<Object> getAllUsers() {
        return ApiResponse.success("All users fetch", adminService.getAllUsers());
    }

    @Tag(name = "Application Management")
    @Operation(summary = "Get all submitted applications for review")
    @GetMapping("/applications/all")
    public ApiResponse<java.util.List<Object>> getSubmittedApplications() {
        return ApiResponse.success("Submitted applications fetched successfully",
                adminService.getSubmittedApplications());
    }

    @Tag(name = "Application Management")
    @Operation(summary = "Get single application by applicant id")
    @GetMapping("/applications/{applicantId}")
    public ApiResponse<Object> getApplicationDetailsByApplicant(
            @PathVariable String applicantId) {
        return ApiResponse.success("Application fetched successfully",
                adminService.getApplicationDetailsForApplicant(applicantId));
    }

    @Tag(name = "Verification & Decision")
    @Operation(summary = "Download all documents as a ZIP")
    @GetMapping(value = "/documents/{applicantId}/download")
    public org.springframework.http.ResponseEntity<byte[]> downloadAllDocuments(
            @PathVariable String applicantId,
            @Parameter(hidden = true) HttpServletRequest request) {
        String adminEmail = request.getHeader(LOGGED_IN_USER);
        String adminId = request.getHeader(APPLICANT_ID_HEADER);

        byte[] zipData = adminService.downloadAllDocumentsZip(applicantId, adminEmail, adminId);

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=applicant_" + applicantId + "_docs.zip")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/zip"))
                .body(zipData);
    }

    @Tag(name = "Verification & Decision")
    @Operation(summary = "Verify applicant documents")
    @PatchMapping("/documents/verify/{applicantId}")
    public ApiResponse<Object> verifyDocuments(
            @PathVariable String applicantId,
            @RequestParam String status,
            @RequestParam String remarks,
            @Parameter(hidden = true) HttpServletRequest request) {
        String adminEmail = request.getHeader(LOGGED_IN_USER);
        String adminId = request.getHeader(APPLICANT_ID_HEADER);

        return ApiResponse.success(
                "Documents verified successfully",
                adminService.verifyDocuments(applicantId, adminId, adminEmail, status, remarks));
    }

    @Tag(name = "Verification & Decision")
    @Operation(summary = "Approve or reject application")
    @PatchMapping("/applications/decision/{applicantId}")
    public ApiResponse<Object> makeDecision(
            @PathVariable String applicantId,
            @io.swagger.v3.oas.annotations.Parameter(schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {
                    STATUS_APPROVED, STATUS_REJECTED })) @RequestParam String decision,
            @RequestParam String remarks,
            @Parameter(hidden = true) HttpServletRequest request) {
        String adminEmail = request.getHeader(LOGGED_IN_USER);
        String adminId = request.getHeader(APPLICANT_ID_HEADER);

        // Normalize decision to match service expectations
        String status = decision.trim().toUpperCase();
        if ("APPROVE".equals(status))
            status = STATUS_APPROVED;
        if ("REJECT".equals(status))
            status = STATUS_REJECTED;

        if (!STATUS_APPROVED.equals(status) && !STATUS_REJECTED.equals(status)) {
            throw new AdminException("Allowed decision statuses: " + STATUS_APPROVED + ", " + STATUS_REJECTED);
        }

        return ApiResponse.success(
                "Decision applied successfully",
                adminService.makeFinalDecision(applicantId, status, adminId, adminEmail, remarks));
    }

    @Tag(name = "Reporting")
    @Operation(summary = "Get personal admin activity report")
    @GetMapping("/reports/my")
    public ApiResponse<Object> getMyReport(@Parameter(hidden = true) HttpServletRequest request) {
        String adminId = request.getHeader(APPLICANT_ID_HEADER);
        return ApiResponse.success("Admin report fetched", adminService.getMyReport(adminId));
    }

}
