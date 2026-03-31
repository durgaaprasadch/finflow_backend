package com.finflow.auth.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void testLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("pass");
        
        assertEquals("test@example.com", request.getEmail());
        assertEquals("pass", request.getPassword());
        
        LoginRequest request2 = new LoginRequest("test2@example.com", "pass2");
        assertEquals("test2@example.com", request2.getEmail());
        assertEquals("pass2", request2.getPassword());
    }

    @Test
    void testLoginResponse() {
        LoginResponse response = new LoginResponse();
        response.setAccessToken("token");
        response.setTokenType("Bearer");
        response.setExpiresIn(3600);
        response.setRole("ADMIN");
        
        assertEquals("token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("ADMIN", response.getRole());
        
        LoginResponse response2 = LoginResponse.builder()
                .accessToken("test-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .role("USER")
                .build();
        assertEquals("test-token", response2.getAccessToken());
        assertEquals("Bearer", response2.getTokenType());
        
        LoginResponse builderResponse = LoginResponse.builder()
                .accessToken("token3")
                .tokenType("Bearer")
                .expiresIn(1800)
                .role("GUEST")
                .build();
        assertEquals("token3", builderResponse.getAccessToken());
        assertEquals("GUEST", builderResponse.getRole());
    }

    @Test
    void testSignupRequest() {
        SignupRequest request = new SignupRequest();
        request.setFullName("Full Name");
        request.setEmail("test@example.com");
        request.setPassword("pass");
        request.setPhone("123456");
        
        assertEquals("Full Name", request.getFullName());
        assertEquals("test@example.com", request.getEmail());
        assertEquals("pass", request.getPassword());
        assertEquals("123456", request.getPhone());
        
        SignupRequest request2 = new SignupRequest("Name", "mail", "pass", "phone");
        assertEquals("Name", request2.getFullName());
        
        SignupRequest builderRequest = SignupRequest.builder()
                .fullName("Full Name")
                .email("test@example.com")
                .password("pass")
                .phone("123456")
                .build();
        assertEquals("Full Name", builderRequest.getFullName());
    }
}
