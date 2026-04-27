package com.fanzzi.backend.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final Key key;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    private static final String ISSUER = "fanzzi-auth";

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessTokenExpiry,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiry
    ) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 chars");
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    // =====================================================
    // 🔹 PARSE & VALIDATE
    // =====================================================

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(ISSUER)
                .setAllowedClockSkewSeconds(30)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // =====================================================
    // 🔹 SAFE EXTRACT
    // =====================================================

    public String getUserId(Claims claims) {
        return claims.getSubject();
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String getType(Claims claims) {
        return claims.get("type", String.class);
    }

    public String getDeviceId(Claims claims) {
        return claims.get("deviceId", String.class);
    }

    public String getSessionId(Claims claims) {
        return claims.get("sessionId", String.class);
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    // =====================================================
    // 🔹 TOKEN GENERATION
    // =====================================================

    public String generateAccessToken(
            String userId,
            String role,
            String deviceId,
            String sessionId
    ) {

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userId)
                .setIssuer(ISSUER)
                .claim("role", role)
                .claim("type", "access")
                .claim("deviceId", deviceId)
                .claim("sessionId", sessionId) // 🔥 CRITICAL
                .setIssuedAt(new Date(now))
                .setNotBefore(new Date(now))   // 🔥 extra safety
                .setExpiration(new Date(now + accessTokenExpiry))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String userId) {

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userId)
                .setIssuer(ISSUER)
                .claim("type", "refresh")
                .setIssuedAt(new Date(now))
                .setNotBefore(new Date(now))
                .setExpiration(new Date(now + refreshTokenExpiry))
                .signWith(key)
                .compact();
    }

    // =====================================================
    // 🔹 VALIDATION
    // =====================================================

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}