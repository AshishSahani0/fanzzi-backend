package com.fanzzi.backend.auth.userauth.service;

import com.fanzzi.backend.auth.device.service.UserDeviceService;
import com.fanzzi.backend.auth.events.UserCreatedEvent;
import com.fanzzi.backend.auth.jwt.JwtUtil;
import com.fanzzi.backend.auth.model.AuthUser;
import com.fanzzi.backend.auth.refresh.service.RefreshTokenService;
import com.fanzzi.backend.auth.repository.AuthUserRepository;
import com.fanzzi.backend.auth.session.dto.UserSessionDTO;
import com.fanzzi.backend.auth.session.service.SessionService;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthService {

    private final AuthUserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final UserDeviceService deviceService;
    private final ApplicationEventPublisher publisher;
    private final StringRedisTemplate redisTemplate;

    // =====================================================
    // 🔐 USER LOGIN
    // =====================================================

    public Map<String, String> login(
            String firebaseIdToken,
            String deviceId,
            String countryCode,
            String ipAddress,
            String userAgent,
            String platform,
            String deviceName,
            String osVersion,
            String appVersion,
            String fcmToken
    ) throws Exception {

        // =====================================================
        // 🔐 BASIC VALIDATION
        // =====================================================

        validateDevice(deviceId);
        validateToken(firebaseIdToken);

        String safeIp = safe(ipAddress, "unknown");
        String safeAgent = safe(userAgent, "unknown");

        // =====================================================
        // 🔐 FIREBASE VERIFY
        // =====================================================

        FirebaseToken decoded;

        try {
            log.info("🚀 BEFORE FIREBASE VERIFY");
            decoded = FirebaseAuth.getInstance()
                    .verifyIdToken(firebaseIdToken);
            log.info("✅ AFTER FIREBASE VERIFY");
        } catch (Exception e) {
            log.warn("LOGIN FAILED firebase ip={}", safeIp);
            throw new ApiException(
                    ErrorCode.INVALID_FIREBASE_TOKEN,
                    "Authentication failed"
            );
        }

        String phone = extractPhone(decoded);
        String normalizedPhone = normalizePhone(phone);

        // =====================================================
        // 👤 FIND OR CREATE USER
        // =====================================================

        AuthUser user = userRepository.findByPhone(normalizedPhone)
                .orElseGet(() -> createUser(normalizedPhone, countryCode));

        // =====================================================
        // 🚫 ACCOUNT CHECK
        // =====================================================

        if (!user.isActive() || user.isBanned()) {
            throw new ApiException(
                    ErrorCode.ACCOUNT_BLOCKED,
                    "Account access denied"
            );
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // =====================================================
        // 📱 DEVICE REGISTER
        // =====================================================

        deviceService.registerDevice(
                user.getId(),
                deviceId,
                fcmToken,
                safe(platform, "ANDROID"),
                safe(deviceName, "unknown-device"),
                safe(osVersion, "unknown-os"),
                safe(appVersion, "1.0.0"),
                safeIp,
                safeAgent
        );

        // =====================================================
        // 🧠 SESSION
        // =====================================================

        sessionService.clearSession(user.getId(), deviceId);

        UserSessionDTO session = sessionService.saveSession(
                user,
                deviceId,
                safeIp,
                safeAgent
        );

        if (session == null || session.getSessionId() == null) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Session creation failed");
        }



        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getRole().name(),
                deviceId,
                session.getSessionId()
        );

        refreshTokenService.revoke(user.getId(), deviceId);

        String refreshToken = refreshTokenService.createRefreshToken(
                user.getId(),
                deviceId
        );

        // =====================================================
        // 📊 LOG
        // =====================================================

        log.info(
                "LOGIN SUCCESS user={} device={} ip={}",
                user.getId(),
                deviceId,
                safeIp
        );

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }

    // =====================================================
    // 🔧 HELPERS
    // =====================================================

    private void validateDevice(String deviceId) {
        if (deviceId == null || deviceId.isBlank() || deviceId.length() < 10) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid device ID");
        }
    }

    private void validateToken(String token) {
        if (token == null || token.length() < 50) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid request");
        }
    }

    private String extractPhone(FirebaseToken decoded) {
        log.info("🔥 FULL CLAIMS: {}", decoded.getClaims());

        Object phoneClaim = decoded.getClaims().get("phone_number");

        if (phoneClaim == null) {
            log.error("❌ PHONE CLAIM MISSING: {}", decoded.getClaims());
            throw new ApiException(
                    ErrorCode.INVALID_FIREBASE_TOKEN,
                    "Phone number missing"
            );
        }

        String phone = phoneClaim.toString().trim();
        log.info("📱 EXTRACTED PHONE: {}", phone);

        if (phone.isEmpty()) {
            throw new ApiException(
                    ErrorCode.INVALID_FIREBASE_TOKEN,
                    "Phone number invalid"
            );
        }

        return phone;
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9+]", "");
    }

    private AuthUser createUser(String phone, String countryCode) {

        AuthUser user = new AuthUser();
        user.setPhone(phone);
        user.setCountryCode(countryCode);
        user.setCreatedAt(Instant.now());
        user.setActive(true);
        user.setBanned(false);

        AuthUser saved = userRepository.save(user);

        publisher.publishEvent(
                new UserCreatedEvent(
                        saved.getId(),
                        saved.getPhone(),
                        saved.getCountryCode()
                )
        );

        return saved;
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}