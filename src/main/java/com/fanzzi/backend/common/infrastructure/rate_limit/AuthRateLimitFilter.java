package com.fanzzi.backend.common.infrastructure.rate_limit;

import com.fanzzi.backend.auth.refresh.util.TokenHashUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;

    // =====================================================
    // 🚦 LIMIT CONFIG
    // =====================================================

    private static final int SOFT_LIMIT = 10;
    private static final int MEDIUM_LIMIT = 20;
    private static final int HARD_LIMIT = 50;

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Duration MEDIUM_BLOCK = Duration.ofMinutes(5);
    private static final Duration HARD_BLOCK = Duration.ofHours(1);

    private static final String PREFIX = "rate:auth:";

    // =====================================================
    // 🔥 FILTER LOGIC
    // =====================================================

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        String ip = safe(extractClientIp(request));
        String deviceId = safe(request.getHeader("X-Device-Id"));
        String phone = safe(request.getHeader("X-Phone"));

        // 🔐 HASH to prevent key explosion & PII leakage
        String fingerprint = TokenHashUtil.sha256(ip + "|" + deviceId + "|" + phone);

        String key = PREFIX + fingerprint;

        Long count = redis.opsForValue().increment(key);

        if (count != null && count == 1) {
            redis.expire(key, WINDOW);
        }

        if (count == null) {
            chain.doFilter(request, response);
            return;
        }

        // =====================================================
        // 🚫 HARD BLOCK
        // =====================================================

        if (count > HARD_LIMIT) {

            redis.expire(key, HARD_BLOCK);

            log.error("🚫 HARD BLOCK fingerprint={}", fingerprint);

            block(response, "Too many requests. Try again later.");
            return;
        }

        // =====================================================
        // ⚠️ MEDIUM BLOCK
        // =====================================================

        if (count > MEDIUM_LIMIT) {

            redis.expire(key, MEDIUM_BLOCK);

            log.warn("⚠️ MEDIUM BLOCK fingerprint={}", fingerprint);

            block(response, "Too many attempts. Please wait a few minutes.");
            return;
        }

        // =====================================================
        // 🐢 SOFT LIMIT
        // =====================================================

        if (count > SOFT_LIMIT) {

            try {
                Thread.sleep(150); // reduced delay (non-blocking impact)
            } catch (InterruptedException ignored) {}
        }

        chain.doFilter(request, response);
    }

    // =====================================================
    // 🎯 FILTER ONLY AUTH ROUTES
    // =====================================================

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return !(path.equals("/auth/user/login")
                || path.equals("/auth/admin/login")
                || path.equals("/auth/admin/google"));
    }

    // =====================================================
    // 🚫 BLOCK RESPONSE
    // =====================================================

    private void block(HttpServletResponse response, String message) throws IOException {

        response.setStatus(429);
        response.setContentType("application/json");

        response.getWriter().write("""
{
  "success": false,
  "errorCode": "TOO_MANY_REQUESTS",
  "message": "%s"
}
""".formatted(message));
    }

    // =====================================================
    // 🌐 REAL IP (PROXY SAFE)
    // =====================================================

    private String extractClientIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    // =====================================================
    // 🔧 SAFE VALUE
    // =====================================================

    private String safe(String val) {
        return (val == null || val.isBlank()) ? "unknown" : val.trim();
    }
}