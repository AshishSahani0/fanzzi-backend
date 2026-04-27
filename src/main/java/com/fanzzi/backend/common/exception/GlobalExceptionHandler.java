package com.fanzzi.backend.common.exception;

import com.fanzzi.backend.common.dto.ErrorResponse;
import com.fanzzi.backend.common.dto.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // =========================================
    // 🔥 Custom API Exception
    // =========================================

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response = baseBuilder(request)
                .errorCode(ex.getErrorCode().name())
                .message(ex.getMessage())
                .build();

        return ResponseEntity
                .status(ex.getStatus())
                .body(response);
    }

    // =========================================
    // 🔐 Security Exception (401)
    // =========================================

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(
            SecurityException ex,
            HttpServletRequest request
    ) {

        ErrorResponse response = baseBuilder(request)
                .errorCode(ErrorCode.UNAUTHORIZED.name())
                .message("Access denied")
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    // =========================================
    // 🧾 Validation Errors
    // =========================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        List<ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ValidationError(
                        err.getField(),
                        err.getDefaultMessage()
                ))
                .toList();

        ErrorResponse response = baseBuilder(request)
                .errorCode(ErrorCode.VALIDATION_ERROR.name())
                .message("Validation failed")
                .validationErrors(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // =========================================
    // 💥 Unknown Errors
    // =========================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(
            Exception ex,
            HttpServletRequest request
    ) {

        String requestId = UUID.randomUUID().toString();

        log.error("Unhandled exception [{}]", requestId, ex);

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.name())
                .message("Something went wrong. Please try again later.")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    // =========================================
    // 🧠 Base Builder
    // =========================================

    private ErrorResponse.ErrorResponseBuilder baseBuilder(
            HttpServletRequest request
    ) {
        return ErrorResponse.builder()
                .success(false)
                .timestamp(Instant.now())
                .path(request.getRequestURI());
    }
}