//package com.fanzzi.backend.auth.adminauth.controller;
//
//import com.fanzzi.backend.auth.adminauth.service.AdminAuthService;
//import com.fanzzi.backend.auth.adminauth.service.AdminOAuthService;
//import com.fanzzi.backend.auth.refresh.service.RefreshTokenService;
//import com.fanzzi.backend.auth.session.service.SessionService;
//import com.fanzzi.backend.common.exception.ApiException;
//import com.fanzzi.backend.common.exception.ErrorCode;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.http.ResponseCookie;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/auth/admin")
//@RequiredArgsConstructor
//@Slf4j
//public class AdminAuthController {
//
//    private final AdminAuthService adminAuthService;
//    private final AdminOAuthService adminOAuthService;
//    private final RefreshTokenService refreshTokenService;
//    private final SessionService sessionService;
//
//    // =====================================================
//    // 🔐 EMAIL LOGIN
//    // =====================================================
//
//    @PostMapping("/login")
//    public Map<String, String> login(
//            @RequestBody Map<String, String> request,
//            HttpServletRequest httpRequest,
//            HttpServletResponse response
//    ) {
//
//        String deviceId = request.get("deviceId");
//        String ip = extractIp(httpRequest);
//
//        Map<String, String> tokens =
//                adminAuthService.login(
//                        request.get("email"),
//                        request.get("password"),
//                        request.get("masterCode"),
//                        deviceId,
//                        ip
//                );
//
//        setCookie(response, tokens.get("refreshToken"));
//
//        return Map.of("accessToken", tokens.get("accessToken"));
//    }
//
//    // =====================================================
//    // 🔐 GOOGLE LOGIN
//    // =====================================================
//
//    @PostMapping("/google")
//    public Map<String, String> googleLogin(
//            @RequestBody Map<String, String> request,
//            HttpServletRequest httpRequest,
//            HttpServletResponse response
//    ) {
//
//        String deviceId = request.get("deviceId");
//        String ip = extractIp(httpRequest);
//
//        Map<String, String> tokens =
//                adminOAuthService.loginWithGoogle(
//                        request.get("firebaseToken"),
//                        deviceId,
//                        ip
//                );
//
//        setCookie(response, tokens.get("refreshToken"));
//
//        return Map.of("accessToken", tokens.get("accessToken"));
//    }
//
//    // =====================================================
//    // 🔄 REFRESH
//    // =====================================================
//
//    @PostMapping("/refresh")
//    public Map<String, String> refresh(
//            @CookieValue(value = "adminRefreshToken", required = false)
//            String refreshToken,
//
//            @RequestHeader("X-Device-Id")
//            String deviceId,
//
//            HttpServletResponse response
//    ) {
//
//        if (refreshToken == null) {
//            throw new ApiException(ErrorCode.NO_REFRESH, "Session expired");
//        }
//
//        Map<String, String> rotated =
//                refreshTokenService.rotate(
//                        refreshToken,
//                        "ADMIN",
//                        deviceId
//                );
//
//        setCookie(response, rotated.get("refreshToken"));
//
//        return Map.of("accessToken", rotated.get("accessToken"));
//    }
//
//    // =====================================================
//    // 🚪 LOGOUT
//    // =====================================================
//
//    @PostMapping("/logout")
//    public Map<String, String> logout(
//            @CookieValue(value = "adminRefreshToken", required = false)
//            String refreshToken,
//
//            @RequestHeader("X-Device-Id")
//            String deviceId,
//
//            HttpServletResponse response
//    ) {
//
//        if (refreshToken == null) {
//            throw new ApiException(ErrorCode.NO_REFRESH, "Already logged out");
//        }
//
//        String adminId =
//                refreshTokenService.getUserIdFromRawToken(refreshToken);
//
//        refreshTokenService.revoke(adminId, deviceId);
//        sessionService.clearSession(adminId, deviceId);
//
//        clearCookie(response);
//
//        return Map.of("message", "Logged out");
//    }
//
//    // =====================================================
//    // 🍪 COOKIE HELPERS
//    // =====================================================
//
//    private void setCookie(HttpServletResponse response, String token) {
//        ResponseCookie cookie = ResponseCookie.from("adminRefreshToken", token)
//                .httpOnly(true)
//                .secure(true)
//                .sameSite("Strict")
//                .path("/auth/admin/refresh")
//                .maxAge(30L * 24 * 60 * 60)
//                .build();
//
//        response.addHeader("Set-Cookie", cookie.toString());
//    }
//
//    private void clearCookie(HttpServletResponse response) {
//        ResponseCookie cookie = ResponseCookie.from("adminRefreshToken", "")
//                .httpOnly(true)
//                .secure(true)
//                .sameSite("Strict")
//                .path("/auth/admin/refresh")
//                .maxAge(0)
//                .build();
//
//        response.addHeader("Set-Cookie", cookie.toString());
//    }
//
//    private String extractIp(HttpServletRequest request) {
//        String forwarded = request.getHeader("X-Forwarded-For");
//        return (forwarded != null) ? forwarded.split(",")[0] : request.getRemoteAddr();
//    }
//}