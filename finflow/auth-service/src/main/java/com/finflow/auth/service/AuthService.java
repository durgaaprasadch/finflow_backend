

package com.finflow.auth.service;
// Refresh triggering comment

import com.finflow.auth.dto.*;
import com.finflow.notification.dto.NotificationRequest;
import com.finflow.auth.entity.User;
import com.finflow.auth.repository.UserRepository;
import com.finflow.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finflow.auth.exception.AuthException;
import com.finflow.auth.exception.UserNotFoundException;

import com.finflow.auth.entity.UserOtp;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
@SuppressWarnings("null")
public class AuthService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING_ADMIN_APPROVAL = "ADMIN_APPROVAL_PENDING";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_APPLICANT = "APPLICANT";
    private static final String STATUS_UNVERIFIED = "UNVERIFIED";
    private static final String MSG_USER_NOT_FOUND = "User not found";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_MESSAGE = "message";
    private static final String TEMPLATE_REGISTRATION = "registration-template";

    private static final Random RANDOM = new Random();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;
    private final com.finflow.auth.repository.UserOtpRepository otpRepository;
    private final AuthService self;

    @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.notification.exchanges.notification:notification-exchange}")
    private String notificationExchange;

    @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.notification.routing-keys.registration:notification.registration}")
    private String registrationRoutingKey;

    @org.springframework.beans.factory.annotation.Value("${spring.rabbitmq.notification.routing-keys.login:notification.login}")
    private String loginRoutingKey;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            @org.springframework.context.annotation.Lazy org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate,
            com.finflow.auth.repository.UserOtpRepository otpRepository,
            @org.springframework.context.annotation.Lazy AuthService self) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rabbitTemplate = rabbitTemplate;
        this.otpRepository = otpRepository;
        this.self = self;
    }

    @Transactional
    public String signup(String registerAs, SignupRequest request) {
        User user = self.registerUser(registerAs, request);
        return "User registered successfully. ID: " + (user != null ? user.getId() : "Unknown");
    }

    @Transactional
    public User registerUser(String registerAs, SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new AuthException("Email already exists");
        }

        String email = request.getEmail().toLowerCase();
        boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(registerAs);

        User user = User.builder()
                .fullName(request.getFullName())
                .email(email)
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(isAdmin ? ROLE_ADMIN : ROLE_APPLICANT)
                .status(STATUS_UNVERIFIED)
                .build();

        User savedUser = userRepository.save(user);
        log.info("CORE_REG: Saved user: {} with role: {} and status: {}", savedUser.getEmail(), savedUser.getRole(), savedUser.getStatus());

        // Registration OTP (Surgery: Now for everyone)
        String regOtp = null;
        // BURN OLD REGISTRATION OTPS before sending new one (Surgical Clean)
        otpRepository.deleteByEmailAndPurpose(email, UserOtp.OtpPurpose.REGISTRATION);

        regOtp = String.format("%06d", RANDOM.nextInt(999999));
        UserOtp userOtp = UserOtp.builder()
                .email(savedUser.getEmail())
                .otp(regOtp)
                .purpose(UserOtp.OtpPurpose.REGISTRATION)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .build();
        otpRepository.save(userOtp);

        try {
            Map<String, Object> model = new java.util.HashMap<>();
            model.put("name", savedUser.getFullName());
            model.put("email", savedUser.getEmail());
            
            if (regOtp != null) {
                model.put("otp", regOtp);
                log.info("DIAGNOSTIC: OTP generated for {} registration: {}", savedUser.getRole(), regOtp);
            }

            NotificationRequest welcomeNotification = NotificationRequest.builder()
                    .to(savedUser.getEmail())
                    .subject(regOtp != null ? "VERIFICATION REQUIRED: Activate Your FinFlow Account" : "Welcome to FinFlow - Registration Successful")
                    .templateName(TEMPLATE_REGISTRATION) 
                    .model(model)
                    .build();

            log.info("RABBIT_SEND: Dispatching email for {} (Role: {}) to Exchange: {}, Key: {}", savedUser.getEmail(), savedUser.getRole(), notificationExchange, registrationRoutingKey);
            rabbitTemplate.convertAndSend(notificationExchange, registrationRoutingKey, welcomeNotification);

            // Admin Alert is now moved to verifyRegistration for security (Must verify email first)
        } catch (Exception e) {
            log.error("RABBIT_FAILED: Could not send registration emails for {}: {}", savedUser.getEmail(), e.getMessage());
        }

        return savedUser;
    }

    @Transactional
    public void resendSignupOtp(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        if (!STATUS_UNVERIFIED.equals(user.getStatus())) {
            throw new AuthException("Account is already verified or active. Status: " + user.getStatus());
        }

        // BURN OLD REGISTRATION OTPS
        otpRepository.deleteByEmailAndPurpose(email.toLowerCase(), UserOtp.OtpPurpose.REGISTRATION);

        String regOtp = String.format("%06d", RANDOM.nextInt(999999));
        UserOtp userOtp = UserOtp.builder()
                .email(user.getEmail())
                .otp(regOtp)
                .purpose(UserOtp.OtpPurpose.REGISTRATION)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .build();
        otpRepository.save(userOtp);

        try {
            Map<String, Object> model = new java.util.HashMap<>();
            model.put("name", user.getFullName());
            model.put("email", user.getEmail());
            model.put("otp", regOtp);

            NotificationRequest welcomeNotification = NotificationRequest.builder()
                    .to(user.getEmail())
                    .subject("ACTION REQUIRED: New Verification Code for FinFlow")
                    .templateName(TEMPLATE_REGISTRATION)
                    .model(model)
                    .build();

            log.info("RABBIT_SEND: Resending OTP for {} (Role: {})", user.getEmail(), user.getRole());
            rabbitTemplate.convertAndSend(notificationExchange, registrationRoutingKey, welcomeNotification);
        } catch (Exception e) {
            log.error("RABBIT_FAILED: Could not resend registration OTP for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Transactional
    public void verifyRegistration(String email, String otp) {
        UserOtp userOtp = otpRepository.findTopByEmailAndPurposeAndVerifiedFalseOrderByExpiryTimeDesc(email.toLowerCase(), UserOtp.OtpPurpose.REGISTRATION)
                .filter(o -> !o.isExpired())
                .filter(o -> o.getOtp().equals(otp))
                .orElseThrow(() -> new AuthException("Invalid or expired OTP"));

        userOtp.setVerified(true);
        otpRepository.save(userOtp);

        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new AuthException(MSG_USER_NOT_FOUND));

        if (STATUS_UNVERIFIED.equals(user.getStatus())) {
            boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(user.getRole());
            user.setStatus(isAdmin ? STATUS_PENDING_ADMIN_APPROVAL : STATUS_ACTIVE);
            userRepository.save(user);
            log.info("VERIFY_REG: User {} (Role: {}) verified email. Status set to: {}", email, user.getRole(), user.getStatus());

            // SEND SUCCESS CONFIRMATION EMAIL
            try {
                NotificationRequest successMail = NotificationRequest.builder()
                        .to(user.getEmail())
                        .subject(isAdmin ? "Email Verified - Admin Approval Pending" : "Account Activated - Welcome to FinFlow!")
                        .templateName(TEMPLATE_REGISTRATION)
                        .model(java.util.Map.of("name", user.getFullName()))
                        .build();
                rabbitTemplate.convertAndSend(notificationExchange, registrationRoutingKey, successMail);
                
                // IF ADMIN, SEND ALERT TO SUPER ADMIN NOW
                if (isAdmin) {
                    NotificationRequest adminAlert = NotificationRequest.builder()
                            .to("durgaprasadch.in@gmail.com")
                            .subject("ACTION REQUIRED: New Admin Verified & Pending Approval")
                            .templateName("admin-alert-template")
                            .model(Map.of(
                                    "name", "Super Admin",
                                    "applicationId", "USER-" + user.getId(),
                                    "status", "ADMIN_VERIFIED_PENDING_APPROVAL",
                                    KEY_TIMESTAMP, LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                            .build();
                    rabbitTemplate.convertAndSend(notificationExchange, registrationRoutingKey, adminAlert);
                    log.info("RABBIT_SEND: Admin Alert dispatched to super-admin for verified admin: {}", email);
                }
            } catch (Exception e) {
                log.error("RABBIT_FAILED: Could not send success/alert emails for {}: {}", email, e.getMessage());
            }
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Invalid credentials");
        }

        // Allow login for ACTIVE, APPROVED, or APPROVE status
        String status = user.getStatus();
        if (!STATUS_ACTIVE.equalsIgnoreCase(status) && !"APPROVED".equalsIgnoreCase(status) && !"APPROVE".equalsIgnoreCase(status)) {
            throw new AuthException("Account is " + status);
        }

        String otp = String.format("%06d", RANDOM.nextInt(999999));
        otpRepository.deleteByEmailAndPurpose(user.getEmail(), UserOtp.OtpPurpose.LOGIN); 
        UserOtp userOtp = UserOtp.builder()
                .email(user.getEmail())
                .otp(otp)
                .purpose(UserOtp.OtpPurpose.LOGIN)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepository.save(userOtp);

        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .to(user.getEmail())
                    .subject("Secured Login Verification - FinFlow")
                    .templateName("otp-template")
                    .model(Map.of(
                            "name", user.getFullName(),
                            "otp", otp,
                            KEY_MESSAGE, "A secure login request"))
                    .build();
            rabbitTemplate.convertAndSend(notificationExchange, loginRoutingKey, notification);
            log.info("RABBIT_SEND: Login OTP dispatched for {}", user.getEmail());
        } catch (Exception e) {
            log.error("RABBIT_FAILED: Failed to send login OTP: {}", e.getMessage());
        }

        return LoginResponse.builder()
                .mfaRequired(true)
                .role(user.getRole())
                .build();
    }

    @Transactional
    public LoginResponse verifyLoginOtp(String email, String otp) {
        UserOtp userOtp = otpRepository.findTopByEmailAndPurposeOrderByExpiryTimeDesc(email.toLowerCase(), UserOtp.OtpPurpose.LOGIN)
                .orElseThrow(() -> new AuthException("Login verification session expired or not found"));

        if (userOtp.isExpired() || !userOtp.getOtp().equals(otp)) {
            throw new AuthException("Invalid or expired verification code");
        }

        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException(MSG_USER_NOT_FOUND));

        // Generate final JWT
        String token = jwtService.generateToken(user.getEmail(), user.getRole(), user.getId());
        
        // Cleanup Login OTP only
        otpRepository.deleteByEmailAndPurpose(user.getEmail(), UserOtp.OtpPurpose.LOGIN);
        
        log.info("AUTH_SUCCESS: 2FA completed for {}", email);

        // DISPATCH SUCCESSFUL LOGIN NOTIFICATION
        try {
            NotificationRequest successNotify = NotificationRequest.builder()
                    .to(email)
                    .subject("New Login Detected - FinFlow Audit")
                    .templateName("login-template")
                    .model(Map.of(
                            "name", user.getFullName(),
                            KEY_TIMESTAMP, LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            "role", user.getRole()))
                    .build();
            rabbitTemplate.convertAndSend(notificationExchange, loginRoutingKey, successNotify);
            log.info("RABBIT_SEND: Login success notification dispatched for {}", email);
        } catch (Exception e) {
            log.error("RABBIT_FAILED: Could not send login success email for {}: {}", email, e.getMessage());
        }

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType(LoginResponse.TOKEN_TYPE_BEARER)
                .expiresIn(86400) // 24h
                .role(user.getRole())
                .mfaRequired(false)
                .build();
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("User not registered with this email"));

        // Generate 6-digit OTP
        String otp = String.format("%06d", RANDOM.nextInt(999999));
        otpRepository.deleteByEmailAndPurpose(email.toLowerCase(), UserOtp.OtpPurpose.FORGOT_PASSWORD);
        UserOtp userOtp = UserOtp.builder()
                .email(email.toLowerCase())
                .otp(otp)
                .purpose(UserOtp.OtpPurpose.FORGOT_PASSWORD)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepository.save(userOtp);

        // Notify User via RabbitMQ
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .to(user.getEmail())
                    .subject("Identity Verification - FinFlow")
                    .templateName("otp-template")
                    .model(Map.of(
                            "name", user.getFullName(),
                            "otp", otp,
                            KEY_MESSAGE, "A password reset request"))
                    .build();
            rabbitTemplate.convertAndSend(notificationExchange, loginRoutingKey, notification);
            log.info("RABBIT_SEND: OTP dispatched for password reset: {}", user.getEmail());
        } catch (Exception e) {
            log.error("RABBIT_FAILED: Failed to send OTP email: {}", e.getMessage());
        }
    }

    @Transactional
    public String verifyOtp(String email, String otp) {
        UserOtp userOtp = otpRepository.findTopByEmailAndPurposeOrderByExpiryTimeDesc(email.toLowerCase(), UserOtp.OtpPurpose.FORGOT_PASSWORD)
                .orElseThrow(() -> new AuthException("No OTP found for this email"));

        if (userOtp.isExpired()) {
            throw new AuthException("OTP has expired. Please request a new one.");
        }
        if (!userOtp.getOtp().equals(otp)) {
            throw new AuthException("Invalid OTP code");
        }

        String resetToken = java.util.UUID.randomUUID().toString();
        userOtp.setVerified(true);
        userOtp.setResetToken(resetToken);
        otpRepository.save(userOtp);
        
        return resetToken;
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new AuthException("Passwords do not match");
        }

        com.finflow.auth.entity.UserOtp userOtp = otpRepository.findByResetToken(resetToken)
                .orElseThrow(() -> new AuthException("Invalid or expired reset token"));

        if (userOtp.isExpired()) {
            otpRepository.delete(userOtp);
            throw new AuthException("Reset token has expired");
        }

        if (!userOtp.isVerified()) {
            throw new AuthException("Identity verification required");
        }

        User user = userRepository.findByEmail(userOtp.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User associated with this token no longer exists"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Notify success
        try {
            Map<String, Object> model = new java.util.HashMap<>();
            model.put("name", user.getFullName());
            model.put(KEY_TIMESTAMP, LocalDateTime.now().toString());
            NotificationRequest notification = NotificationRequest.builder()
                    .to(user.getEmail())
                    .subject("Password Changed Successfully - FinFlow")
                    .templateName("password-reset-success-template")
                    .model(model)
                    .build();
            rabbitTemplate.convertAndSend(notificationExchange, registrationRoutingKey, notification);
        } catch (Exception e) {
            log.error("Failed to send password reset success email: {}", e.getMessage());
        }

        // Cleanup Reset OTP only
        otpRepository.deleteByEmailAndPurpose(user.getEmail(), UserOtp.OtpPurpose.FORGOT_PASSWORD);
        log.info("USER_MGMT: Password reset successful via token for user: {}", user.getEmail());
    }

    @Transactional
    public void requestDeleteAccount(String email, String password) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new AuthException(MSG_USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthException("Identity verification failed: Invalid password");
        }

        otpRepository.deleteByEmailAndPurpose(email.toLowerCase(), UserOtp.OtpPurpose.DELETE_ACCOUNT);
        String otp = String.format("%06d", RANDOM.nextInt(999999));
        
        UserOtp userOtp = UserOtp.builder()
                .email(email.toLowerCase())
                .otp(otp)
                .purpose(UserOtp.OtpPurpose.DELETE_ACCOUNT)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .build();
        otpRepository.save(userOtp);

        try {
            com.finflow.notification.dto.NotificationRequest notification = com.finflow.notification.dto.NotificationRequest.builder()
                    .to(email)
                    .subject("CRITICAL: Confirm Final Account Deletion - FinFlow")
                    .templateName("delete-account-template")
                    .model(java.util.Map.of(
                            "name", user.getFullName(),
                            "otp", otp))
                    .build();
            rabbitTemplate.convertAndSend(notificationExchange, registrationRoutingKey, notification);
            log.info("RABBIT_SEND: Delete OTP dispatched for {}", email);
        } catch (Exception e) {
            log.error("RABBIT_FAILED: Could not send delete OTP for {}: {}", email, e.getMessage());
        }
    }

    @Transactional
    public void verifyDeleteAccount(String email, String otp) {
        UserOtp userOtp = otpRepository.findTopByEmailAndPurposeAndVerifiedFalseOrderByExpiryTimeDesc(email.toLowerCase(), UserOtp.OtpPurpose.DELETE_ACCOUNT)
                .filter(o -> !o.isExpired() && o.getOtp().equals(otp))
                .orElseThrow(() -> new AuthException("Invalid or expired deletion code"));

        userOtp.setVerified(true);
        otpRepository.save(userOtp);

        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new AuthException(MSG_USER_NOT_FOUND));
        
        // Final Farewell Email
        try {
            com.finflow.notification.dto.NotificationRequest farewell = com.finflow.notification.dto.NotificationRequest.builder()
                    .to(email)
                    .subject("Final Confirmation: FinFlow Account & Data Purged")
                    .templateName("account-purged-template")
                    .model(Map.of("name", user.getFullName()))
                    .build();
            rabbitTemplate.convertAndSend(notificationExchange, registrationRoutingKey, farewell);
            log.info("RABBIT_SEND: Dispatching final farewell for {}", email);
        } catch (Exception e) {
            log.error("RABBIT_FAILED: Could not send farewell email for {}: {}", email, e.getMessage());
        }

        // Final cleanup
        otpRepository.deleteByEmail(email.toLowerCase());
        userRepository.delete(user);
        log.warn("CORE_AUTH: User {} PERMANENTLY DELETED their account from DB", email);
    }

    @Transactional
    public void updateUserStatus(String userId, String status) {
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new AuthException("User not found: " + userId));

        String normalizedStatus = status.trim().toUpperCase();
        if ("APPROVE".equals(normalizedStatus) || "APPROVED".equals(normalizedStatus)) {
            normalizedStatus = STATUS_ACTIVE;
        }

        user.setStatus(normalizedStatus);
        userRepository.save(user);
        log.info("USER_MGMT: Status updated for user: {} to {} (Internal State: {})", userId, status, normalizedStatus);
        
        // Notify of account decision (Approval/Rejection/etc.)
        try {
            boolean isAdmin = ROLE_ADMIN.equalsIgnoreCase(user.getRole());
            boolean isApproved = STATUS_ACTIVE.equalsIgnoreCase(normalizedStatus);
            
            String subject = determineAccountStatusSubject(isAdmin, isApproved);
            String template = isAdmin ? "admin-alert-template" : TEMPLATE_REGISTRATION;
            
            String message = determineAccountStatusMessage(isAdmin, isApproved, status);

            NotificationRequest activationMail = NotificationRequest.builder()
                    .to(user.getEmail())
                    .subject(subject)
                    .templateName(template)
                    .model(Map.of(
                        "name", user.getFullName(),
                        "status", isAdmin ? "ADMIN_" + status.toUpperCase() : status.toUpperCase(),
                        KEY_MESSAGE, message
                    ))
                    .build();
            rabbitTemplate.convertAndSend(notificationExchange, registrationRoutingKey, activationMail);
            log.info("RABBIT_SEND: Account status notification ({}) sent to {} ({})", status, user.getEmail(), user.getRole());
        } catch (Exception e) {
            log.error("RABBIT_FAILED: Account status mail failed for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String determineAccountStatusSubject(boolean isAdmin, boolean isApproved) {
        if (isAdmin) {
            return isApproved ? "Administrator Account Approved - FinFlow" : "Update: Your Administrator Account Status";
        }
        return isApproved ? "Account Activated! Your FinFlow Access is Live" : "Update: Your FinFlow Application Status";
    }

    private String determineAccountStatusMessage(boolean isAdmin, boolean isApproved, String originalStatus) {
        if (isApproved) {
            return isAdmin ? "Your administrative credentials have been approved and are now active." : "Your application account is now fully active.";
        }
        return "An administrative decision has been made regarding your " + (isAdmin ? "admin" : "applicant") + " account. Current status: " + originalStatus.toUpperCase();
    }

    public List<User> getPendingAdminUsers() {
        return userRepository.findAllByRoleAndStatus(ROLE_ADMIN, STATUS_PENDING_ADMIN_APPROVAL);
    }

    public List<User> getAllAdminUsers() {
        return userRepository.findAllByRole(ROLE_ADMIN);
    }

    public List<User> getActiveAdminUsers() {
        return userRepository.findAllByRoleAndStatus(ROLE_ADMIN, STATUS_ACTIVE);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
