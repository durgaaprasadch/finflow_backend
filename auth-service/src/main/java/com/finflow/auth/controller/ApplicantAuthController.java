package com.finflow.auth.controller;

import com.finflow.auth.dto.ApiResponse;
import com.finflow.auth.dto.LoginRequest;
import com.finflow.auth.dto.LoginResponse;
import com.finflow.auth.dto.LoginResponseData;
import com.finflow.auth.dto.PasswordResetRequest;
import com.finflow.auth.dto.RegisterRole;
import com.finflow.auth.dto.SignupRequest;
import com.finflow.auth.dto.SignupResponseData;
import com.finflow.auth.entity.User;
import com.finflow.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class ApplicantAuthController {

    private final AuthService authService;

    public ApplicantAuthController(AuthService authService) {
        this.authService = authService;
    }

    @Tag(name = "Registration & Activation")
    @Operation(summary = "Register applicant or admin user")
    @PostMapping("/signup/{registerAs}")
    public ResponseEntity<ApiResponse<SignupResponseData>> signup(
            @PathVariable RegisterRole registerAs,
            @RequestBody SignupRequest request) {
        User user = authService.registerUser(registerAs.name(), request);
        String message = "ADMIN".equalsIgnoreCase(user.getRole())
                ? "Admin registration pending Super Admin approval."
                : "Registration successful. Please verify your email with the OTP sent to you.";
        return ResponseEntity.status(201).body(ApiResponse.success(
                message,
                new SignupResponseData(user.getId(), user.getStatus())));
    }

    @Tag(name = "Registration & Activation")
    @Operation(summary = "Verify Registration OTP", description = "Activate your account by verifying the 6-digit code received via email.")
    @PostMapping("/signup/verify")
    public ResponseEntity<ApiResponse<String>> verifySignup(
            @RequestBody PasswordResetRequest.VerifyRequest request) {
        authService.verifyRegistration(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success(
                "Account activated successfully. You can now login.", "ACTIVE"));
    }

    @Tag(name = "Registration & Activation")
    @Operation(summary = "Resend Registration OTP", description = "If you didn't receive the initial verification code, use this to request a new one.")
    @PostMapping("/signup/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(@RequestParam String email) {
        authService.resendSignupOtp(email);
        return ResponseEntity.ok(ApiResponse.success("A new verification code has been sent to " + email, null));
    }

    @Tag(name = "Secure Login & 2FA")
    @Operation(summary = "Login user and initiate 2FA")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseData>> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(
                response.isMfaRequired() ? "2FA_REQUIRED: Please enter the code sent to your email"
                        : "Login successful",
                new LoginResponseData(response.getAccessToken(), response.isMfaRequired())));
    }

    @Tag(name = "Secure Login & 2FA")
    @Operation(summary = "Verify Login OTP", description = "Complete the 2FA login by verifying the OTP received via email.")
    @PostMapping("/login/verify")
    public ResponseEntity<ApiResponse<LoginResponseData>> verifyLogin(
            @RequestBody PasswordResetRequest.VerifyRequest request) {
        LoginResponse response = authService.verifyLoginOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success(
                "Login successful",
                new LoginResponseData(response.getAccessToken(), false)));
    }

    @Tag(name = "Account Recovery")
    @Operation(summary = "Forgot Password", description = "Initiate password reset by sending a 6-digit OTP to user email.")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody PasswordResetRequest.ForgotRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok("OTP sent successfully to " + request.getEmail());
    }

    @Tag(name = "Account Recovery")
    @Operation(summary = "Verify Reset OTP", description = "Verify the 6-digit OTP and receive a secure Reset Token.")
    @PostMapping("/verify-otp")
    public ResponseEntity<java.util.Map<String, String>> verifyOtp(
            @RequestBody PasswordResetRequest.VerifyRequest request) {
        String resetToken = authService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(java.util.Map.of(
                "message", "OTP verified successfully",
                "resetToken", resetToken));
    }

    @Tag(name = "Account Recovery")
    @Operation(summary = "Reset Password", description = "Reset the user's password using the secure Reset Token.")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetRequest.ResetRequest request) {
        authService.resetPassword(request.getResetToken(), request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok("Password reset successfully. Please login with your new password.");
    }

    @Tag(name = "User Account Management")
    @Operation(summary = "Request account deletion OTP (Public)")
    @PostMapping("/delete-request")
    public ResponseEntity<ApiResponse<String>> requestDelete(
            @RequestParam String email,
            @RequestParam String password) {
        authService.requestDeleteAccount(email, password);
        return ResponseEntity.ok(ApiResponse.success("Account deletion OTP sent to your email", null));
    }

    @Tag(name = "User Account Management")
    @Operation(summary = "Verify OTP and PERMANENTLY delete account (Public)")
    @DeleteMapping("/delete-verify")
    public ResponseEntity<ApiResponse<String>> verifyDelete(
            @RequestParam String email,
            @RequestParam String otp) {
        authService.verifyDeleteAccount(email, otp);
        return ResponseEntity.ok(ApiResponse.success("Account permanently removed from FinFlow system", null));
    }
}
