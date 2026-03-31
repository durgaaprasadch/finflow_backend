package com.finflow.auth.dto;
// Refresh triggering comment

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    @NotBlank(message = "Full name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]{2,100}$", message = "Full name must contain only alphabets and spaces, between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", 
             message = "Password must be at least 8 characters long and contain at least one uppercase, one lowercase, one digit, and one special character (@#$%^&+=)")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number (must be 10 digits starting with 6-9)")
    private String phone;
}
