package com.finflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finflow.auth.dto.LoginRequest;
import com.finflow.auth.dto.LoginResponse;
import com.finflow.auth.dto.SignupRequest;
import com.finflow.auth.entity.User;
import com.finflow.auth.service.AuthService;
import com.finflow.auth.config.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicantAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @SuppressWarnings("null")
    void signup_ShouldReturnApplicantContractResponse() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .fullName("Durga")
                .email("durga@gmail.com")
                .password("Strong@123")
                .phone("9876543210")
                .build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .role("APPLICANT")
                .status("ACTIVE")
                .build();
        when(authService.registerUser(anyString(), any(SignupRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/signup/APPLICANT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful. Please verify your email with the OTP sent to you."))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @SuppressWarnings("null")
    void login_ShouldReturnOnlyAccessTokenInData() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .accessToken("jwt-token")
                .role("APPLICANT")
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest("durga@gmail.com", "Strong@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));
    }
}
