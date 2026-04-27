package com.fanzzi.backend.common.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
public class ErrorResponse {

    private boolean success;

    private String errorCode;

    private String message;

    private Instant timestamp;

    private String path;

    private String requestId; // ⭐ useful for debugging

    private List<ValidationError> validationErrors;
}