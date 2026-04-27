package com.fanzzi.backend.common.exception;

public enum ErrorCode {

    // ===== General =====
    BAD_REQUEST,
    NOT_FOUND,
    UNAUTHORIZED,
    FORBIDDEN,
    CONFLICT,
    INTERNAL_SERVER_ERROR,INTERNAL_ERROR,
    INVALID_CHANNEL,
    EMAIL_ALREADY_EXISTS,
    INVALID_POST,
    INVALID_ACTION,
    COMMENT_NOT_FOUND,

    // ===== Validation =====
    VALIDATION_ERROR,
    NO_CHANGES_DETECTED,

    // ===== Auth / OTP =====
    PHONE_EXISTS,
    RATE_LIMIT,
    OTP_NOT_FOUND,
    INVALID_OTP,
    OTP_RATE_LIMIT,

    USER_NOT_FOUND,
    ACCOUNT_BLOCKED,

    // ===== Token / Session =====
    NO_REFRESH,
    INVALID_REFRESH_TOKEN,
    REFRESH_INVALID,
    REFRESH_EXPIRED,
    SESSION_EXPIRED,
    DEVICE_MISMATCH,

    // ===== Admin =====
    ADMIN_LOGIN_FAILED,
    ADMIN_DENIED,
    ADMIN_INVALID_CREDENTIALS,


    //====User=======
    USERNAME_TAKEN,

    // ===== Firebase =====
    INVALID_FIREBASE_TOKEN,

    // ===== Appeals =====
    APPEAL_NOT_FOUND,
    APPEAL_ALREADY_DECIDED,
    APPEAL_EXISTS,
    NOT_BANNED,

    // ===== Channel =====
    CHANNEL_NOT_FOUND,
    CHANNEL_ALREADY_BLOCKED,
    CHANNEL_NOT_BLOCKED,
    INVALID_REQUEST,
    SERVER_ERROR,
    POST_NOT_FOUND,


    ACTIVE,
    EXPIRED,
    GRACE_PERIOD,
    CANCELED,
    REFUNDED,
    TOO_MANY_REQUESTS
}