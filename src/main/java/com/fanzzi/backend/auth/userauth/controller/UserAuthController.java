package com.fanzzi.backend.auth.userauth.controller;

import com.fanzzi.backend.auth.refresh.service.RefreshTokenService;
import com.fanzzi.backend.auth.session.service.SessionService;
import com.fanzzi.backend.auth.userauth.service.UserAuthService;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/user")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;

    // =====================================================
    // 🔐 LOGIN
    // =====================================================

    @PostMapping("/login")
    public Map<String, String> login(
            @RequestHeader("Authorization") String firebaseHeader,
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestHeader("X-Country-Code") String countryCode,
            @RequestHeader(value = "X-Platform", required = false) String platform,
            @RequestHeader(value = "X-Device-Name", required = false) String deviceName,
            @RequestHeader(value = "X-OS-Version", required = false) String osVersion,
            @RequestHeader(value = "X-App-Version", required = false) String appVersion,
            @RequestHeader(value = "X-FCM-Token", required = false) String fcmToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {

        if (firebaseHeader == null || !firebaseHeader.startsWith("Bearer ")) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid Firebase token");
        }

        String firebaseToken = firebaseHeader.substring(7);

        String ip = extractClientIp(request);
        String agent = request.getHeader("User-Agent");

        Map<String, String> tokens =
                userAuthService.login(
                        firebaseToken,
                        deviceId,
                        countryCode,
                        ip,
                        agent,
                        platform,
                        deviceName,
                        osVersion,
                        appVersion,
                        fcmToken
                );

        // 🍪 REFRESH COOKIE
        ResponseCookie cookie =
                ResponseCookie.from("userRefreshToken", tokens.get("refreshToken"))
                        .httpOnly(true)
                        .secure(false) // 🔥 set true in production (HTTPS)
                        .sameSite("Lax")
                        .path("/auth/user/refresh")
                        .maxAge(30L * 24 * 60 * 60)
                        .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return Map.of("accessToken", tokens.get("accessToken"));
    }

    // =====================================================
    // 🔄 REFRESH
    // =====================================================

    @PostMapping("/refresh")
    public Map<String, String> refresh(
            @CookieValue(value = "userRefreshToken", required = false)
            String refreshToken,

            @RequestHeader("X-Device-Id")
            String deviceId,

            HttpServletResponse response
    ) {

        if (refreshToken == null) {
            throw new ApiException(ErrorCode.NO_REFRESH, "Session expired");
        }

        Map<String, String> rotated =
                refreshTokenService.rotate(refreshToken, "USER", deviceId);

        ResponseCookie cookie =
                ResponseCookie.from("userRefreshToken", rotated.get("refreshToken"))
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/auth/user/refresh")
                        .maxAge(30L * 24 * 60 * 60)
                        .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return Map.of("accessToken", rotated.get("accessToken"));
    }

    // =====================================================
    // 🚪 LOGOUT
    // =====================================================

    @PostMapping("/logout")
    public Map<String, String> logout(
            @CookieValue(value = "userRefreshToken", required = false)
            String refreshToken,

            @RequestHeader("X-Device-Id")
            String deviceId,

            HttpServletResponse response
    ) {

        if (refreshToken == null) {
            throw new ApiException(ErrorCode.NO_REFRESH, "Already logged out");
        }

        String userId =
                refreshTokenService.getUserIdFromRawToken(refreshToken);

        refreshTokenService.revoke(userId, deviceId);
        sessionService.clearSession(userId, deviceId);

        ResponseCookie clearCookie =
                ResponseCookie.from("userRefreshToken", "")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/auth/user/refresh")
                        .maxAge(0)
                        .build();

        response.addHeader("Set-Cookie", clearCookie.toString());

        return Map.of("message", "Logged out successfully");
    }

    private String extractClientIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}