package com.finflow.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/signup/**",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/internal/admin/**",
                                "/api/v1/auth/signup/**",
                                "/api/v1/auth/login",
                                "/api/v1/auth/login/verify",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/verify-otp",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/delete-request",
                                "/api/v1/auth/delete-verify",
                                "/api/auth/internal/**"
                        ).permitAll()
                        .requestMatchers(
                                "/v3/api-docs", 
                                "/v3/api-docs/**",
                                "/api/v1/auth/v3/api-docs",
                                "/api/v1/auth/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/auth/admin/approve/**", "/api/v1/auth/admin/approve/**").hasAuthority("ADMIN")
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
