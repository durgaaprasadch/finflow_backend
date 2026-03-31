package com.finflow.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";
    private final long expiration = 3600000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expiration", expiration);
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken("test@example.com", "ROLE_USER", userId);
        assertNotNull(token);
        assertEquals("test@example.com", jwtService.extractEmail(token));
        assertEquals("ROLE_USER", jwtService.extractRole(token));
    }

    @Test
    void generateRefreshToken_ShouldReturnValidToken() {
        String token = jwtService.generateRefreshToken("test@example.com");
        assertNotNull(token);
        assertEquals("test@example.com", jwtService.extractEmail(token));
    }
}
