package com.finflow.application.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApplicationException_ShouldReturnBadRequest() {
        ApplicationException ex = new ApplicationException("App error");
        ResponseEntity<Map<String, Object>> response = handler.handleApplicationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("App error", body.get("message"));
        assertEquals("Application Business Error", body.get("error"));
    }

    @Test
    void handleInvalidTransition_ShouldReturnBadRequest() {
        InvalidTransitionException ex = new InvalidTransitionException("Transition error");
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidTransition(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Transition error", body.get("message"));
        assertEquals("Bad Request", body.get("error"));
    }

    @Test
    void handleRuntimeExceptions_WithUnauthorized_ShouldReturnForbidden() {
        RuntimeException ex = new RuntimeException("Unauthorized: Access denied");
        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeExceptions(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Unauthorized: Access denied", body.get("message"));
        assertEquals("Forbidden", body.get("error"));
    }

    @Test
    void handleRuntimeExceptions_General_ShouldReturnBadRequest() {
        RuntimeException ex = new RuntimeException("General error");
        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("General error", body.get("message"));
        assertEquals("Bad Request", body.get("error"));
    }

    @Test
    void handleRuntimeExceptions_NullMessage_ShouldReturnBadRequest() {
        RuntimeException ex = new RuntimeException((String) null);
        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(null, body.get("message"));
    }
}
