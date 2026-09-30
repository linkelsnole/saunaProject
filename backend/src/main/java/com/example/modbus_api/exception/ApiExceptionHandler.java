package com.example.modbus_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Неверное имя пользователя или пароль");
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        return buildResponse(HttpStatus.FORBIDDEN, "Доступ запрещен");
    }

    private ResponseEntity<Map<String, String>> buildResponse(HttpStatus status, String message) {
        Map<String, String> body = new LinkedHashMap<String, String>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
