package com.finflow.auth.repository;

import com.finflow.auth.entity.UserOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserOtpRepository extends JpaRepository<UserOtp, Long> {
    Optional<UserOtp> findTopByEmailAndPurposeOrderByExpiryTimeDesc(String email, UserOtp.OtpPurpose purpose);
    Optional<UserOtp> findTopByEmailAndPurposeAndVerifiedFalseOrderByExpiryTimeDesc(String email, UserOtp.OtpPurpose purpose);
    Optional<UserOtp> findByResetToken(String resetToken);
    void deleteByEmail(String email);
    void deleteByEmailAndPurpose(String email, UserOtp.OtpPurpose purpose);
}
