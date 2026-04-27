package com.fanzzi.backend.common.messaging.websocket.interceptor;

import com.fanzzi.backend.auth.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    private static final String TOKEN_PARAM = "token=";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {

        try {
            String token = extractToken(request);

            if (token == null || !jwtUtil.isValid(token)) {
                reject(response, "Invalid token");
                return false;
            }

            Claims claims = jwtUtil.parseClaims(token);

            // ✅ ONLY ACCESS TOKENS
            if (!"access".equals(jwtUtil.getType(claims))) {
                reject(response, "Invalid token type");
                return false;
            }

            String userId = jwtUtil.getUserId(claims);
            String sessionId = jwtUtil.getSessionId(claims);
            String deviceId = jwtUtil.getDeviceId(claims);

            if (userId == null || deviceId == null) {
                reject(response, "Missing identity");
                return false;
            }

            // =====================================================
            // ✅ ATTACH IDENTITY (FAST PATH)
            // =====================================================
            attributes.put("userId", userId);
            attributes.put("sessionId", sessionId);
            attributes.put("deviceId", deviceId);

            return true;

        } catch (Exception e) {
            reject(response, "Auth error");
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op (keep lightweight)
    }

    // =====================================================
    // 🔐 SAFE TOKEN EXTRACTION
    // =====================================================
    private String extractToken(ServerHttpRequest request) {

        String query = request.getURI().getRawQuery();
        if (query == null) return null;

        for (String param : query.split("&")) {

            if (param.startsWith(TOKEN_PARAM)) {

                String value = param.substring(TOKEN_PARAM.length());

                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }

        return null;
    }

    // =====================================================
    // 🚫 REJECT HANDSHAKE (IMPORTANT)
    // =====================================================
    private void reject(ServerHttpResponse response, String reason) {

        if (response instanceof ServletServerHttpResponse servletResponse) {
            servletResponse.getServletResponse().setStatus(HttpStatus.UNAUTHORIZED.value());
        }

        // optional: log reason
        // log.warn("WS Auth failed: {}", reason);
    }
}