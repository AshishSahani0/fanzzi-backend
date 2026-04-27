//package com.fanzzi.backend.auth.adminauth.service;
//
//import com.fanzzi.backend.auth.jwt.JwtUtil;
//import com.fanzzi.backend.auth.refresh.service.RefreshTokenService;
//import com.fanzzi.backend.common.exception.ApiException;
//import com.fanzzi.backend.common.exception.ErrorCode;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseToken;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AdminOAuthService {
//
//    private final JwtUtil jwtUtil;
//    private final RefreshTokenService refreshTokenService;
//
//    // 🔐 Whitelisted admins ONLY
//    private static final List<String> ALLOWED_ADMINS = List.of(
//            "you@fanzzi.com",
//            "admin@fanzzi.com"
//    );
//
//    public Map<String, String> loginWithGoogle(
//            String firebaseToken,
//            String deviceId,
//            String ip
//    ) {
//
//        if (deviceId == null || deviceId.isBlank()) {
//            throw new ApiException(ErrorCode.BAD_REQUEST, "Device required");
//        }
//
//        FirebaseToken decoded;
//
//        try {
//            decoded = FirebaseAuth.getInstance()
//                    .verifyIdToken(firebaseToken);
//        } catch (Exception e) {
//            log.warn("ADMIN GOOGLE LOGIN FAILED ip={}", ip);
//            throw new ApiException(
//                    ErrorCode.INVALID_FIREBASE_TOKEN,
//                    "Authentication failed"
//            );
//        }
//
//        String email = decoded.getEmail();
//
//        if (email == null || !ALLOWED_ADMINS.contains(email)) {
//            log.warn("ADMIN UNAUTHORIZED GOOGLE LOGIN email={} ip={}", email, ip);
//            throw new ApiException(
//                    ErrorCode.ADMIN_DENIED,
//                    "Access denied"
//            );
//        }
//
//        // 🔥 generate tokens
//        String accessToken = jwtUtil.generateAccessToken(
//                "ADMIN",
//                "ADMIN",
//                deviceId
//        );
//
//        refreshTokenService.revoke("ADMIN", deviceId);
//
//        String refreshToken =
//                refreshTokenService.createRefreshToken("ADMIN", deviceId);
//
//        log.info("ADMIN GOOGLE LOGIN SUCCESS email={} ip={}", email, ip);
//
//        return Map.of(
//                "accessToken", accessToken,
//                "refreshToken", refreshToken
//        );
//    }
//}