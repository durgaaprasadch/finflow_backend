package com.finflow.document.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDocumentException_ShouldReturnForbidden() {
        DocumentException ex = new DocumentException("Storage error");
        ResponseEntity<Map<String, Object>> response = handler.handleDocumentException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Storage error", body.get("message"));
    }

    @Test
    void handleRuntimeExceptions_ShouldReturnInternalServerError() {
        RuntimeException ex = new RuntimeException("Runtime error");
        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeExceptions(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Runtime error", body.get("message"));
    }

    @Test
    void handleGeneralExceptions_ShouldReturnInternalServerError() {
        Exception ex = new Exception("General error");
        ResponseEntity<Map<String, Object>> response = handler.handleGeneralExceptions(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("An error occurred: General error", body.get("message"));
    }
}
