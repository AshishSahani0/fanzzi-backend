package com.fanzzi.backend.auth.refresh.service;

import com.fanzzi.backend.auth.jwt.JwtUtil;
import com.fanzzi.backend.auth.refresh.util.TokenHashUtil;
import com.fanzzi.backend.auth.session.dto.UserSessionDTO;
import com.fanzzi.backend.auth.session.service.SessionService;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final SessionService sessionService; // 🔥 NEW

    private static final Duration TTL = Duration.ofDays(30);
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);

    private String key(String userId, String deviceId) {
        return "refresh:" + userId + ":" + deviceId;
    }

    private String devicesKey(String userId) {
        return "refresh:" + userId + ":devices";
    }

    private String jtiKey(String jti) {
        return "refresh:jti:" + jti;
    }

    private String rateKey(String userId, String deviceId) {
        return "rate:refresh:" + userId + ":" + deviceId;
    }

    // =====================================================
    // CREATE REFRESH TOKEN
    // =====================================================

    public String createRefreshToken(String userId, String deviceId) {

        validateDevice(deviceId);

        String rawToken = jwtUtil.generateRefreshToken(userId);
        String hash = TokenHashUtil.sha256(rawToken);

        redis.opsForValue().set(key(userId, deviceId), hash, TTL);

        redis.opsForSet().add(devicesKey(userId), deviceId);
        redis.expire(devicesKey(userId), TTL);

        return rawToken;
    }

    // =====================================================
    // ROTATE TOKEN
    // =====================================================

    public Map<String, String> rotate(
            String rawToken,
            String role,
            String deviceId
    ) {

        validateDevice(deviceId);

        Claims claims;

        try {
            claims = jwtUtil.parseClaims(rawToken);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.REFRESH_INVALID, "Invalid token");
        }

        if (!"refresh".equals(jwtUtil.getType(claims))) {
            throw new ApiException(ErrorCode.REFRESH_INVALID, "Invalid token type");
        }

        String userId = jwtUtil.getUserId(claims);
        String jti = jwtUtil.getJti(claims);

        if (userId == null || jti == null) {
            throw new ApiException(ErrorCode.REFRESH_INVALID, "Invalid token");
        }

        // =====================================================
        // 🔐 SESSION FETCH (🔥 NEW)
        // =====================================================

        UserSessionDTO session = sessionService.getSession(userId, deviceId);

        if (session == null ||
                !session.isActive() ||
                session.isBanned() ||
                session.isDeleted()) {

            throw new ApiException(ErrorCode.REFRESH_EXPIRED, "Session expired");
        }

        // =====================================================
        // ⚡ RATE LIMIT
        // =====================================================

        String rateKey = rateKey(userId, deviceId);

        Long count = redis.opsForValue().increment(rateKey);

        if (count != null && count == 1) {
            redis.expire(rateKey, RATE_WINDOW);
        }

        if (count != null && count > 20) {
            log.warn("REFRESH RATE LIMIT user={} device={}", userId, deviceId);
            throw new ApiException(ErrorCode.TOO_MANY_REQUESTS, "Too many attempts");
        }

        // =====================================================
        // 🚨 REPLAY DETECTION
        // =====================================================

        if (Boolean.TRUE.equals(redis.hasKey(jtiKey(jti)))) {

            log.error("🚨 TOKEN REPLAY DETECTED user={} device={}", userId, deviceId);

            revokeAll(userId);

            throw new ApiException(
                    ErrorCode.REFRESH_INVALID,
                    "Security violation detected"
            );
        }

        // =====================================================
        // 🔐 VERIFY HASH
        // =====================================================

        String storedHash = redis.opsForValue().get(key(userId, deviceId));

        if (storedHash == null) {
            throw new ApiException(ErrorCode.REFRESH_EXPIRED, "Session expired");
        }

        String incomingHash = TokenHashUtil.sha256(rawToken);

        if (!TokenHashUtil.secureEquals(storedHash, incomingHash)) {

            log.error("🚨 TOKEN MISMATCH user={} device={}", userId, deviceId);

            revokeAll(userId);

            throw new ApiException(ErrorCode.REFRESH_INVALID, "Invalid token");
        }

        // =====================================================
        // 🔥 MARK USED
        // =====================================================

        redis.opsForValue().set(jtiKey(jti), "USED", TTL);

        redis.delete(key(userId, deviceId));

        // =====================================================
        // 🔄 ISSUE NEW TOKENS
        // =====================================================

        String newRefresh = createRefreshToken(userId, deviceId);

        String newAccess = jwtUtil.generateAccessToken(
                userId,
                role,
                deviceId,
                session.getSessionId() // 🔥 CRITICAL FIX
        );

        log.info("REFRESH SUCCESS user={} device={}", userId, deviceId);

        return Map.of(
                "accessToken", newAccess,
                "refreshToken", newRefresh
        );
    }

    // =====================================================
    // LOGOUT DEVICE
    // =====================================================

    public void revoke(String userId, String deviceId) {

        redis.delete(key(userId, deviceId));
        redis.opsForSet().remove(devicesKey(userId), deviceId);

        log.info("LOGOUT DEVICE user={} device={}", userId, deviceId);
    }

    // =====================================================
    // LOGOUT ALL DEVICES
    // =====================================================

    public void revokeAll(String userId) {

        Set<String> deviceIds = redis.opsForSet().members(devicesKey(userId));

        if (deviceIds != null) {
            for (String deviceId : deviceIds) {
                redis.delete(key(userId, deviceId));
            }
        }

        redis.delete(devicesKey(userId));

        log.warn("LOGOUT ALL DEVICES user={}", userId);
    }

    // =====================================================
    // VALIDATION
    // =====================================================

    private void validateDevice(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Device required");
        }
    }

    // =====================================================
// 🔍 EXTRACT USER FROM REFRESH TOKEN
// =====================================================

    public String getUserIdFromRawToken(String rawToken) {

        Claims claims;

        try {
            claims = jwtUtil.parseClaims(rawToken);
        } catch (Exception e) {
            throw new ApiException(
                    ErrorCode.REFRESH_INVALID,
                    "Invalid token"
            );
        }

        // ✅ Ensure it's a refresh token
        if (!"refresh".equals(jwtUtil.getType(claims))) {
            throw new ApiException(
                    ErrorCode.REFRESH_INVALID,
                    "Invalid token type"
            );
        }

        String userId = jwtUtil.getUserId(claims);

        if (userId == null) {
            throw new ApiException(
                    ErrorCode.REFRESH_INVALID,
                    "Invalid token"
            );
        }

        return userId;
    }
}