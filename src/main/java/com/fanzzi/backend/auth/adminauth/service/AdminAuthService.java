//package com.fanzzi.backend.auth.adminauth.service;
//
//import com.fanzzi.backend.auth.jwt.JwtUtil;
//import com.fanzzi.backend.auth.refresh.service.RefreshTokenService;
//import com.fanzzi.backend.common.exception.ApiException;
//import com.fanzzi.backend.common.exception.ErrorCode;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//
//import java.util.Map;
//import java.util.Objects;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AdminAuthService {
//
//    private final JwtUtil jwtUtil;
//    private final RefreshTokenService refreshTokenService;
//    private final PasswordEncoder passwordEncoder;
//
//    @Value("${admin.email}")
//    private String adminEmail;
//
//    @Value("${admin.password}")
//    private String adminPasswordHash;
//
//    @Value("${admin.master-code}")
//    private String masterCode;
//
//    // =====================================================
//    // 🔐 ADMIN LOGIN (PRODUCTION SAFE)
//    // =====================================================
//    public Map<String, String> login(
//            String email,
//            String password,
//            String code,
//            String deviceId,
//            String ipAddress
//    ) {
//
//        // =====================================================
//        // 🔐 BASIC VALIDATION
//        // =====================================================
//
//        if (deviceId == null || deviceId.isBlank() || deviceId.length() < 10) {
//            throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid device");
//        }
//
//        String safeIp = (ipAddress == null || ipAddress.isBlank())
//                ? "unknown"
//                : ipAddress;
//
//        // =====================================================
//        // 🔐 EMAIL CHECK
//        // =====================================================
//
//        if (!Objects.equals(email, adminEmail)) {
//            log.warn("ADMIN LOGIN FAILED (email) ip={}", safeIp);
//            throw new ApiException(
//                    ErrorCode.ADMIN_DENIED,
//                    "Access denied"
//            );
//        }
//
//        // =====================================================
//        // 🔐 PASSWORD CHECK (HASHED)
//        // =====================================================
//
//        if (!passwordEncoder.matches(password, adminPasswordHash)) {
//            log.warn("ADMIN LOGIN FAILED (password) ip={}", safeIp);
//            throw new ApiException(
//                    ErrorCode.ADMIN_INVALID_CREDENTIALS,
//                    "Invalid credentials"
//            );
//        }
//
//        // =====================================================
//        // 🔐 MASTER CODE CHECK
//        // =====================================================
//
//        if (!Objects.equals(code, masterCode)) {
//            log.warn("ADMIN LOGIN FAILED (masterCode) ip={}", safeIp);
//            throw new ApiException(
//                    ErrorCode.ADMIN_DENIED,
//                    "Access denied"
//            );
//        }
//
//        // =====================================================
//        // 🔥 TOKEN GENERATION (DEVICE BOUND)
//        // =====================================================
//
//        String accessToken =
//                jwtUtil.generateAccessToken(
//                        "ADMIN",
//                        "ADMIN",
//                        deviceId
//                );
//
//        // 🔥 prevent multiple refresh tokens
//        refreshTokenService.revoke("ADMIN", deviceId);
//
//        String refreshToken =
//                refreshTokenService.createRefreshToken(
//                        "ADMIN",
//                        deviceId
//                );
//
//        // =====================================================
//        // 📊 LOG SUCCESS
//        // =====================================================
//
//        log.info("ADMIN LOGIN SUCCESS deviceId={} ip={}", deviceId, safeIp);
//
//        return Map.of(
//                "accessToken", accessToken,
//                "refreshToken", refreshToken
//        );
//    }
//}