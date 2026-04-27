package com.fanzzi.backend.auth.jwt;

import com.fanzzi.backend.auth.model.AuthUser;
import com.fanzzi.backend.auth.refresh.util.TokenHashUtil;
import com.fanzzi.backend.auth.session.dto.UserSessionDTO;
import com.fanzzi.backend.auth.session.service.SessionService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SessionService sessionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return path.startsWith("/auth/")
                || path.startsWith("/public/")
                || path.equals("/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        Claims claims;

        try {
            claims = jwtUtil.parseClaims(token);
        } catch (Exception e) {
            unauthorized(response, "Invalid or expired token");
            return;
        }

        // =====================================================
        // 🔐 BASIC VALIDATION
        // =====================================================

        if (!"access".equals(jwtUtil.getType(claims))) {
            unauthorized(response, "Invalid token type");
            return;
        }

        String userId = jwtUtil.getUserId(claims);
        String role = jwtUtil.getRole(claims);
        String tokenDeviceId = jwtUtil.getDeviceId(claims);
        String tokenSessionId = jwtUtil.getSessionId(claims); // 🔥 NEW

        if (userId == null || role == null || tokenDeviceId == null || tokenSessionId == null) {
            unauthorized(response, "Invalid token payload");
            return;
        }

        String authority = "ROLE_" + role;

        // =====================================================
        // 🔹 ADMIN (NO DEVICE CHECK)
        // =====================================================

        if ("ADMIN".equals(role)) {
            setAuth(userId, authority);
            filterChain.doFilter(request, response);
            return;
        }

        // =====================================================
        // 🔹 DEVICE VALIDATION
        // =====================================================

        String deviceId = request.getHeader("X-Device-Id");

        if (deviceId == null || deviceId.isBlank()) {
            unauthorized(response, "Device ID required");
            return;
        }

        if (!deviceId.equals(tokenDeviceId)) {
            unauthorized(response, "Device mismatch");
            return;
        }

        // =====================================================
        // 🔹 SESSION VALIDATION (CRITICAL)
        // =====================================================

        UserSessionDTO session =
                sessionService.getSession(userId, deviceId);

        if (session == null ||
                !session.isActive() ||
                session.isBanned() ||
                session.isDeleted()) {

            unauthorized(response, "Session expired");
            return;
        }

        // 🔥 NEW: SESSION ID MATCH
        if (!tokenSessionId.equals(session.getSessionId())) {
            unauthorized(response, "Invalid session");
            return;
        }

        // =====================================================
        // 🔐 DEVICE FINGERPRINT CHECK
        // =====================================================

        String agent = request.getHeader("User-Agent");

        if (session.getFingerprint() != null && agent != null) {

            String currentFingerprint =
                    TokenHashUtil.sha256(agent);

            if (!TokenHashUtil.secureEquals(
                    session.getFingerprint(),
                    currentFingerprint
            )) {
                unauthorized(response, "Device mismatch");
                return;
            }
        }

        // =====================================================
        // 🌐 IP DRIFT HANDLING
        // =====================================================

        String currentIp = extractClientIp(request);

        if (session.getIpAddress() != null &&
                !session.getIpAddress().equals(currentIp)) {

            System.out.println("IP changed user=" + userId +
                    " old=" + session.getIpAddress() +
                    " new=" + currentIp);

            sessionService.saveSession(
                    buildUser(userId, session),
                    deviceId,
                    currentIp,
                    agent
            );
        }

        // =====================================================
        // ✅ SUCCESS
        // =====================================================

        setAuth(userId, authority);
        filterChain.doFilter(request, response);
    }

    // =====================================================
    // 🔧 HELPERS
    // =====================================================

    private void setAuth(String userId, String authority) {

        var auth = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void unauthorized(HttpServletResponse response, String message)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        response.getWriter().write("""
{
  "success": false,
  "errorCode": "UNAUTHORIZED",
  "message": "%s"
}
""".formatted(message));
    }

    private String extractClientIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private AuthUser buildUser(String userId, UserSessionDTO session) {

        AuthUser user = new AuthUser();
        user.setId(userId);
        user.setActive(session.isActive());
        user.setBanned(session.isBanned());
        return user;
    }
}