package com.finflow.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAuthException_ShouldReturnUnauthorized() {
        AuthException ex = new AuthException("Auth error");
        ResponseEntity<Map<String, String>> response = handler.handleAuthException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("Auth error", body.get("error"));
    }

    @Test
    void handleUserNotFoundException_ShouldReturnNotFound() {
        UserNotFoundException ex = new UserNotFoundException("User not found");
        ResponseEntity<Map<String, String>> response = handler.handleUserNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("User not found", body.get("error"));
    }

    @Test
    void handleGeneralException_ShouldReturnInternalServerError() {
        Exception ex = new Exception("General error");
        ResponseEntity<Map<String, String>> response = handler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("An unexpected error occurred: General error", body.get("error"));
    }

    @Test
    void handleRuntimeException_WithInvalidAccess_ShouldReturnUnauthorized() {
        RuntimeException ex = new RuntimeException("Invalid access");
        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid access", body.get("error"));
    }

    @Test
    void handleRuntimeException_WithNotFoundMessage_ShouldReturnNotFound() {
        RuntimeException ex = new RuntimeException("User not found in system");
        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("User not found in system", body.get("error"));
    }

    @Test
    void handleRuntimeException_WithOtherMessage_ShouldReturnInternalServerError() {
        RuntimeException ex = new RuntimeException("Some random error");
        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertEquals("Some random error", body.get("error"));
    }

    @Test
    void handleRuntimeException_WithNullMessage_ShouldReturnInternalServerError() {
        RuntimeException ex = new RuntimeException((String) null);
        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, String> body = response.getBody();
        assertNotNull(body);
        assertNull(body.get("error"));
    }
}
