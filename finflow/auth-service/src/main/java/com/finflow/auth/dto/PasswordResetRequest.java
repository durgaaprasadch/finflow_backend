package com.finflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class PasswordResetRequest {
    private PasswordResetRequest() {
        throw new IllegalStateException("Utility class");
    }

    @Data
    public static class ForgotRequest {
        @NotBlank @Email private String email;
    }

    @Data
    public static class VerifyRequest {
        @NotBlank @Email private String email;
        @NotBlank @Size(min = 6, max = 6) private String otp;
    }

    @Data
    public static class ResetRequest {
        @NotBlank private String resetToken;
        @NotBlank @Size(min = 8) private String newPassword;
        @NotBlank private String confirmPassword;
    }
}
