//package com.fanzzi.backend.auth.otp.service;
//
//import com.fanzzi.backend.auth.otp.limiter.OtpRateLimiter;
//import com.fanzzi.backend.common.exception.ApiException;
//import com.fanzzi.backend.common.exception.ErrorCode;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.context.annotation.Profile;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//
//@Service
//@Profile("prod")
//@RequiredArgsConstructor
//@Slf4j
//public class RedisOtpRateLimiter implements OtpRateLimiter {
//
//    private final StringRedisTemplate redis;
//
//    private static final int PHONE_LIMIT = 3;
//    private static final int IP_LIMIT = 10;
//    private static final int DEVICE_LIMIT = 5;
//
//    private static final Duration WINDOW = Duration.ofMinutes(1);
//    private static final Duration BLOCK_5_MIN = Duration.ofMinutes(5);
//    private static final Duration BLOCK_1_HOUR = Duration.ofHours(1);
//
//    private static final String PREFIX = "otp:";
//
//    @Override
//    public void checkLimit(String phone, String ip, String deviceId) {
//
//        phone = normalize(phone);
//        ip = safe(ip);
//        deviceId = safe(deviceId);
//
//        // =====================================================
//        // 🔥 PHONE LIMIT
//        // =====================================================
//        enforce(
//                PREFIX + "phone:" + phone,
//                PHONE_LIMIT,
//                "Too many OTP requests for this number"
//        );
//
//        // =====================================================
//        // 🔥 IP LIMIT
//        // =====================================================
//        enforce(
//                PREFIX + "ip:" + ip,
//                IP_LIMIT,
//                "Too many OTP requests from this IP"
//        );
//
//        // =====================================================
//        // 🔥 DEVICE LIMIT
//        // =====================================================
//        enforce(
//                PREFIX + "device:" + deviceId,
//                DEVICE_LIMIT,
//                "Too many OTP requests from this device"
//        );
//    }
//
//    // =====================================================
//    // 🔥 CORE LOGIC (PROGRESSIVE BLOCK)
//    // =====================================================
//
//    private void enforce(String key, int limit, String message) {
//
//        Long count = redis.opsForValue().increment(key);
//
//        if (count != null && count == 1) {
//            redis.expire(key, WINDOW);
//        }
//
//        if (count == null) return;
//
//        // =====================================================
//        // 🚫 HARD BLOCK
//        // =====================================================
//
//        if (count > 50) {
//
//            redis.expire(key, BLOCK_1_HOUR);
//
//            log.error("🚨 OTP HARD BLOCK key={}", key);
//
//            throw new ApiException(
//                    ErrorCode.TOO_MANY_REQUESTS,
//                    "Too many attempts. Try again later."
//            );
//        }
//
//        // =====================================================
//        // ⚠️ MEDIUM BLOCK
//        // =====================================================
//
//        if (count > 20) {
//
//            redis.expire(key, BLOCK_5_MIN);
//
//            log.warn("⚠️ OTP MEDIUM BLOCK key={}", key);
//
//            throw new ApiException(
//                    ErrorCode.TOO_MANY_REQUESTS,
//                    message + ". Please wait a few minutes."
//            );
//        }
//
//        // =====================================================
//        // 🐢 SOFT LIMIT
//        // =====================================================
//
//        if (count > limit) {
//
//            try {
//                Thread.sleep(200); // slow attacker
//            } catch (InterruptedException ignored) {}
//
//            throw new ApiException(
//                    ErrorCode.OTP_RATE_LIMIT,
//                    message + ". Please wait 1 minute."
//            );
//        }
//    }
//
//    // =====================================================
//    // 🔧 HELPERS
//    // =====================================================
//
//    private String normalize(String phone) {
//        return phone == null ? "" : phone.replaceAll("[^0-9+]", "");
//    }
//
//    private String safe(String val) {
//        return (val == null || val.isBlank()) ? "unknown" : val;
//    }
//}