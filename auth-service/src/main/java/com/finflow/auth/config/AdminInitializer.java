package com.finflow.auth.config;

import com.finflow.auth.entity.User;
import com.finflow.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class to initialize the system with a default administrator user.
 * This runner ensures that the environment has at least one functional admin
 * account for bootstrap operations.
 * 
 * @author Durga Prasad
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class AdminInitializer {

    private final String adminEmail;
    private final String defaultAdminPassword;
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String STATUS_ACTIVE = "ACTIVE";

    /**
     * Initializes the admin user credentials from environment variables.
     * 
     * @param adminEmail the admin user's identity (defaults to durgaprasadch.in@gmail.com)
     * @param defaultAdminPassword the initial password (defaults to DurgaManaged@2024)
     */
    public AdminInitializer(
            @Value("${APP_ADMIN_EMAIL:durgaprasadch.in@gmail.com}") String adminEmail,
            @Value("${APP_ADMIN_PASSWORD:DurgaManaged@2024}") String defaultAdminPassword) {
        this.adminEmail = adminEmail;
        this.defaultAdminPassword = defaultAdminPassword;
    }

    @Bean
    @SuppressWarnings("null")
    CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                log.info("Default admin user not found. Creating {}...", adminEmail);
                User admin = User.builder()
                        .fullName("Durga")
                        .email(adminEmail)
                        .password(passwordEncoder.encode(defaultAdminPassword))
                        .role(ROLE_ADMIN)
                        .status(STATUS_ACTIVE)
                        .phone("9876543210") // Default placeholder
                        .build();
                userRepository.save(admin);
                log.info("Default {} created successfully.", adminEmail);
            } else {
                log.info("Default admin user {} already exists.", adminEmail);
            }
        };
    }
}
