package com.finflow.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secret = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437"; // 256-bit secret

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
    }

    @Test
    void validateToken_ShouldNotThrowException_WhenTokenIsValid() {
        String token = generateToken("testUser", "ROLE_USER");
        assertDoesNotThrow(() -> jwtUtil.validateToken(token));
    }

    @Test
    void extractUsername_ShouldReturnCorrectSubject() {
        String token = generateToken("testUser", "ROLE_USER");
        assertEquals("testUser", jwtUtil.extractUsername(token));
    }

    @Test
    void extractRole_ShouldReturnCorrectRole() {
        String token = generateToken("testUser", "ROLE_ADMIN");
        assertEquals("ROLE_ADMIN", jwtUtil.extractRole(token));
    }

    private String generateToken(String subject, String role) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
