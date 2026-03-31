package com.finflow.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_otps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOtp {

    public enum OtpPurpose { LOGIN, FORGOT_PASSWORD, REGISTRATION, DELETE_ACCOUNT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String otp;

    @Column(unique = true)
    private String resetToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;

    @Column(nullable = false)
    private LocalDateTime expiryTime;

    @Builder.Default
    @Column(nullable = false)
    private boolean verified = false;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
}
