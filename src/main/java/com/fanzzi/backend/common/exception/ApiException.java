package com.fanzzi.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public ApiException(ErrorCode code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public ApiException(ErrorCode code, String message, HttpStatus status) {
        super(message);
        this.errorCode = code;
        this.status = status;
    }

    public ApiException(ErrorCode code) {
        this(code, code.name(), HttpStatus.BAD_REQUEST);
    }
}