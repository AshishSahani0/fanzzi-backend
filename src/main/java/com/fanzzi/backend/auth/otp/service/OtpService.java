//package com.fanzzi.backend.auth.otp.service;
//
//import com.fanzzi.backend.auth.otp.limiter.OtpRateLimiter;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class OtpService {
//
//    private final OtpRateLimiter limiter;
//
//    public void sendOtp(
//            String phone,
//            HttpServletRequest request
//    ) {
//
//        String ip = extractClientIp(request);
//        String deviceId = request.getHeader("X-Device-Id");
//
//        limiter.checkLimit(phone, ip, deviceId);
//
//        // =====================================================
//        // 🔐 SEND OTP HERE
//        // =====================================================
//        // Firebase / Twilio / SMS API
//    }
//
//    private String extractClientIp(HttpServletRequest request) {
//
//        String forwarded = request.getHeader("X-Forwarded-For");
//
//        if (forwarded != null && !forwarded.isBlank()) {
//            return forwarded.split(",")[0].trim();
//        }
//
//        return request.getRemoteAddr();
//    }
//}