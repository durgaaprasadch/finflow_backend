package com.finflow.admin.exception;

import com.finflow.admin.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleHttpException_ShouldReturnSameStatusCode() {
        HttpStatusCodeException ex = mock(HttpStatusCodeException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(ex.getStatusText()).thenReturn("Not Found");

        ResponseEntity<ApiResponse<Object>> response = handler.handleHttpException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ApiResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Proxy error: Not Found", body.getMessage());
    }

    @Test
    void handleRuntimeExceptions_ShouldReturnInternalServerError() {
        RuntimeException ex = new RuntimeException("Runtime error");
        ResponseEntity<ApiResponse<Object>> response = handler.handleRuntimeExceptions(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Runtime error", body.getMessage());
    }
}
