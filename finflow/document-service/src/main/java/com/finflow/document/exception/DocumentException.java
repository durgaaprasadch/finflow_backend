package com.finflow.document.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class DocumentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DocumentException(String message) {
        super(message);
    }
    public DocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
