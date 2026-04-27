package com.fanzzi.backend.user.service;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.security.SecurityUtil;
import com.fanzzi.backend.user.model.User;
import com.fanzzi.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserPhoneService {

    private final UserRepository repo;
    private final StringRedisTemplate redis;

    private static final String OTP_KEY = "fanzzi:otp:phone:";
    private static final String RATE_KEY = "fanzzi:otp:rate:";
    private static final String ATTEMPT_KEY = "fanzzi:otp:attempt:";

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_TTL = Duration.ofSeconds(60);

    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom random = new SecureRandom();

    // =====================================================
    // 📲 SEND OTP
    // =====================================================
    public void sendOtp(String phone) {

        phone = normalize(phone);

        if (repo.existsByPhone(phone)) {
            throw new ApiException(
                    ErrorCode.PHONE_EXISTS,
                    "Phone already in use"
            );
        }

        // 🚫 Rate limit (1 per minute)
        Boolean allowed = redis.opsForValue().setIfAbsent(
                RATE_KEY + phone,
                "1",
                RATE_TTL
        );

        if (Boolean.FALSE.equals(allowed)) {
            throw new ApiException(
                    ErrorCode.RATE_LIMIT,
                    "Please wait before requesting OTP"
            );
        }

        String otp = generateOtp();

        redis.opsForValue().set(
                OTP_KEY + phone,
                otp,
                OTP_TTL
        );

        // Reset attempts
        redis.delete(ATTEMPT_KEY + phone);

        // 🔥 TODO: Integrate SMS provider
        System.out.println("OTP for " + phone + " = " + otp);
    }

    // =====================================================
    // 🔐 VERIFY & CHANGE PHONE
    // =====================================================
    public void verifyAndChange(String phone, String otp) {

        phone = normalize(phone);

        String otpKey = OTP_KEY + phone;

        String storedOtp = redis.opsForValue().get(otpKey);

        if (storedOtp == null) {
            throw new ApiException(
                    ErrorCode.OTP_NOT_FOUND,
                    "OTP expired or not requested"
            );
        }

        // 🔥 Attempt limiting
        Long attempts = redis.opsForValue().increment(
                ATTEMPT_KEY + phone
        );

        if (attempts != null && attempts == 1) {
            redis.expire(ATTEMPT_KEY + phone, OTP_TTL);
        }

        if (attempts != null && attempts > MAX_ATTEMPTS) {
            throw new ApiException(
                    ErrorCode.RATE_LIMIT,
                    "Too many invalid attempts"
            );
        }

        if (!storedOtp.equals(otp)) {
            throw new ApiException(
                    ErrorCode.INVALID_OTP,
                    "Invalid OTP"
            );
        }

        // ✅ OTP valid → update phone
        String userId = SecurityUtil.getCurrentUserId();

        User user = repo.findById(userId)
                .orElseThrow(() ->
                        new ApiException(
                                ErrorCode.USER_NOT_FOUND,
                                "User not found"
                        ));

        user.setPhone(phone);
        repo.save(user);

        // 🧹 Cleanup
        redis.delete(otpKey);
        redis.delete(ATTEMPT_KEY + phone);
    }


    private String generateOtp() {
        int number = random.nextInt(900000) + 100000;
        return String.valueOf(number);
    }


    private String normalize(String phone) {
        return phone.trim().replaceAll("\\s+", "");
    }
}