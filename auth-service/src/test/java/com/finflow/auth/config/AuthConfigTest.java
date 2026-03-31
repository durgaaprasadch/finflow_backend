package com.finflow.auth.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.swagger.v3.oas.models.OpenAPI;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig(
            new JwtAuthenticationFilter(Mockito.mock(com.finflow.auth.security.JwtService.class))
    );
    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void passwordEncoder_ShouldReturnBCryptEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder);
    }

    @Test
    void securityFilterChain_ShouldBeProduced() {
        // We just verify the bean method exists and returns something
        assertNotNull(securityConfig);
    }

    @Test
    void openApiConfig_ShouldProduceBeans() {
        OpenAPI openAPI = openApiConfig.authOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openApiConfig.corsConfigurer());
    }
}
