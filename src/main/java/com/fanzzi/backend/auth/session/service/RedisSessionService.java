package com.fanzzi.backend.auth.session.service;

import com.fanzzi.backend.auth.refresh.util.TokenHashUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanzzi.backend.auth.model.AuthUser;
import com.fanzzi.backend.auth.session.dto.UserSessionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Profile("prod")
@RequiredArgsConstructor
public class RedisSessionService implements SessionService {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    private static final Duration TTL = Duration.ofDays(30);

    // =====================================================
    // 🔑 REDIS KEYS
    // =====================================================

    private String key(String userId, String deviceId) {
        return "session:user:" + userId + ":" + deviceId;
    }

    private String devicesKey(String userId) {
        return "session:user:" + userId + ":devices";
    }

    // =====================================================
    // 🔹 GET SESSION (FAST + TTL REFRESH ONLY)
    // =====================================================

    @Override
    public UserSessionDTO getSession(String userId, String deviceId) {

        String redisKey = key(userId, deviceId);

        try {
            String json = redis.opsForValue().get(redisKey);

            if (json == null) return null;

            // ⚡ TTL refresh only (no rewrite)
            redis.expire(redisKey, TTL);

            return mapper.readValue(json, UserSessionDTO.class);

        } catch (Exception e) {
            throw new RuntimeException("Redis session read failed", e);
        }
    }

    // =====================================================
    // 🔹 SAVE SESSION (SMART + MINIMAL WRITE)
    // =====================================================

    @Override
    public UserSessionDTO saveSession(
            AuthUser user,
            String deviceId,
            String ipAddress,
            String userAgent
    ) {

        String safeIp = safe(ipAddress);
        String safeAgent = safe(userAgent);

        String redisKey = key(user.getId(), deviceId);

        try {

            // =====================================================
            // 🔍 READ EXISTING SESSION (IF EXISTS)
            // =====================================================

            String existingJson = redis.opsForValue().get(redisKey);
            UserSessionDTO existing = null;

            if (existingJson != null) {
                existing = mapper.readValue(existingJson, UserSessionDTO.class);
            }

            // =====================================================
            // 🔐 DEVICE FINGERPRINT (ONLY USER-AGENT)
            // =====================================================

            String fingerprint = TokenHashUtil.sha256(safeAgent);

            // =====================================================
            // 🧠 BUILD SESSION
            // =====================================================

            UserSessionDTO dto = UserSessionDTO.builder()
                    .userId(user.getId())
                    .deviceId(deviceId)
                    .active(user.isActive())
                    .banned(user.isBanned())
                    .deleted(false)
                    .fingerprint(fingerprint)
                    .ipAddress(safeIp)
                    .lastIpChangeAt(Instant.now())
                    .userAgent(safeAgent)
                    .lastAccessAt(Instant.now())
                    .build();

            // =====================================================
            // 🌐 IP DRIFT HANDLING (NO FORCE LOGOUT)
            // =====================================================

            if (existing != null) {

                dto.setIpAddress(
                        existing.getIpAddress() != null
                                ? existing.getIpAddress()
                                : safeIp
                );

                dto.setLastIpChangeAt(existing.getLastIpChangeAt());

                // 🔥 detect change only
                if (existing.getIpAddress() != null &&
                        !existing.getIpAddress().equals(safeIp)) {

                    dto.setIpAddress(safeIp);
                    dto.setLastIpChangeAt(Instant.now());
                }

            } else {
                dto.setIpAddress(safeIp);
                dto.setLastIpChangeAt(Instant.now());
            }

            // =====================================================
            // ⚡ WRITE ONLY IF CHANGED (IMPORTANT OPTIMIZATION)
            // =====================================================

            if (existing != null &&
                    TokenHashUtil.secureEquals(existing.getFingerprint(), dto.getFingerprint()) &&
                    Objects.equals(existing.getIpAddress(), dto.getIpAddress())) {

                // only update TTL → NO WRITE
                redis.expire(redisKey, TTL);
                return existing;
            }

            // =====================================================
            // 💾 WRITE TO REDIS
            // =====================================================

            redis.opsForValue().set(
                    redisKey,
                    mapper.writeValueAsString(dto),
                    TTL
            );

            redis.opsForSet().add(devicesKey(user.getId()), deviceId);
            redis.expire(devicesKey(user.getId()), TTL);

            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Redis session save failed", e);
        }
    }

    // =====================================================
    // 🚪 CLEAR SESSION
    // =====================================================

    @Override
    public void clearSession(String userId, String deviceId) {

        redis.delete(key(userId, deviceId));

        redis.opsForSet().remove(
                devicesKey(userId),
                deviceId
        );
    }

    // =====================================================
    // 🔹 GET ALL SESSIONS
    // =====================================================

    @Override
    public List<UserSessionDTO> getAllSessions(String userId) {

        Set<String> deviceIds =
                redis.opsForSet().members(devicesKey(userId));

        if (deviceIds == null || deviceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserSessionDTO> sessions = new ArrayList<>();

        for (String deviceId : deviceIds) {

            UserSessionDTO session = getSession(userId, deviceId);

            if (session != null) {
                sessions.add(session);
            }
        }

        return sessions;
    }

    // =====================================================
    // 🚪 CLEAR ALL SESSIONS
    // =====================================================

    @Override
    public void clearAllSessions(String userId) {

        Set<String> deviceIds =
                redis.opsForSet().members(devicesKey(userId));

        if (deviceIds != null) {
            for (String deviceId : deviceIds) {
                redis.delete(key(userId, deviceId));
            }
        }

        redis.delete(devicesKey(userId));
    }

    // =====================================================
    // 🔧 HELPERS
    // =====================================================

    private String safe(String value) {
        return (value == null || value.isBlank())
                ? "unknown"
                : value.trim();
    }
}