package com.telecom.platform.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static ApiError apiError(String type, String title, int status, String detail,
                                     HttpServletRequest req, List<ApiError.FieldError> errors) {
        return new ApiError(type, title, status, detail, req.getRequestURI(),
                req.getHeader("X-Correlation-Id"), null, Instant.now(), errors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        ApiError error = apiError("https://api.telecom.platform/errors/validation", "Validation Failed",
                ErrorCode.VALIDATION_ERROR.getStatus().value(), "Request validation failed", req, errors);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus()).body(error);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBind(BindException ex, HttpServletRequest req) {
        List<ApiError.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        ApiError error = apiError("https://api.telecom.platform/errors/validation", "Validation Failed",
                ErrorCode.VALIDATION_ERROR.getStatus().value(), "Request validation failed", req, errors);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus()).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        List<ApiError.FieldError> errors = ex.getConstraintViolations().stream()
                .map(cv -> new ApiError.FieldError(cv.getPropertyPath().toString(), cv.getMessage()))
                .collect(Collectors.toList());
        ApiError error = apiError("https://api.telecom.platform/errors/validation", "Validation Failed",
                ErrorCode.VALIDATION_ERROR.getStatus().value(), ex.getMessage(), req, errors);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus()).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        ApiError error = apiError("https://api.telecom.platform/errors/not-found", "Not Found",
                ErrorCode.NOT_FOUND.getStatus().value(), ex.getMessage(), req, null);
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getStatus()).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        ApiError error = apiError("https://api.telecom.platform/errors/forbidden", "Forbidden",
                ErrorCode.FORBIDDEN.getStatus().value(), "Access denied", req, null);
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getStatus()).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        ApiError error = apiError("https://api.telecom.platform/errors/unauthorized", "Unauthorized",
                ErrorCode.UNAUTHORIZED.getStatus().value(), "Authentication required", req, null);
        return ResponseEntity.status(ErrorCode.UNAUTHORIZED.getStatus()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        ApiError error = apiError("https://api.telecom.platform/errors/internal", "Internal Server Error",
                ErrorCode.INTERNAL_ERROR.getStatus().value(), "An unexpected error occurred", req, null);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus()).body(error);
    }
}
