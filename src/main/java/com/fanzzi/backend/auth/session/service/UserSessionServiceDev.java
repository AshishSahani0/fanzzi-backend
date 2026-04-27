package com.fanzzi.backend.auth.session.service;

import com.fanzzi.backend.auth.model.AuthUser;
import com.fanzzi.backend.auth.repository.AuthUserRepository;
import com.fanzzi.backend.auth.refresh.util.TokenHashUtil;
import com.fanzzi.backend.auth.session.dto.UserSessionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class UserSessionServiceDev implements SessionService {

    private final AuthUserRepository repo;

    // 🔥 In-memory session store (dev only)
    private final Map<String, UserSessionDTO> sessions = new HashMap<>();

    private String key(String userId, String deviceId) {
        return userId + ":" + deviceId;
    }

    // =====================================================
    // 🔹 GET SESSION
    // =====================================================

    @Override
    public UserSessionDTO getSession(String userId, String deviceId) {

        String key = key(userId, deviceId);

        UserSessionDTO session = sessions.get(key);

        if (session != null) {
            session.setLastAccessAt(Instant.now()); // simulate activity
            return session;
        }

        return repo.findById(userId)
                .map(user -> buildNew(user, deviceId, "127.0.0.1", "dev-agent"))
                .orElse(null);
    }

    // =====================================================
    // 🔹 SAVE SESSION
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

        String key = key(user.getId(), deviceId);

        UserSessionDTO existing = sessions.get(key);

        String fingerprint = TokenHashUtil.sha256(safeAgent);

        UserSessionDTO session;

        if (existing != null) {

            // 🔄 update existing
            existing.setLastAccessAt(Instant.now());

            if (!Objects.equals(existing.getIpAddress(), safeIp)) {
                existing.setIpAddress(safeIp);
                existing.setLastIpChangeAt(Instant.now());
            }

            session = existing;

        } else {

            session = buildNew(user, deviceId, safeIp, safeAgent);
            session.setFingerprint(fingerprint);

            sessions.put(key, session);
        }

        return session;
    }

    // =====================================================
    // 🔧 CREATE NEW SESSION
    // =====================================================

    private UserSessionDTO buildNew(
            AuthUser user,
            String deviceId,
            String ip,
            String agent
    ) {

        return UserSessionDTO.builder()
                .userId(user.getId())
                .deviceId(deviceId)
                .sessionId(UUID.randomUUID().toString()) // 🔥 CRITICAL
                .active(user.isActive())
                .banned(user.isBanned())
                .deleted(false)
                .fingerprint(TokenHashUtil.sha256(agent))
                .ipAddress(ip)
                .lastIpChangeAt(Instant.now())
                .userAgent(agent)
                .lastAccessAt(Instant.now())
                .build();
    }

    // =====================================================
    // 🔹 CLEAR SESSION
    // =====================================================

    @Override
    public void clearSession(String userId, String deviceId) {
        sessions.remove(key(userId, deviceId));
    }

    // =====================================================
    // 🔹 GET ALL SESSIONS
    // =====================================================

    @Override
    public List<UserSessionDTO> getAllSessions(String userId) {

        List<UserSessionDTO> result = new ArrayList<>();

        for (UserSessionDTO session : sessions.values()) {
            if (session.getUserId().equals(userId)) {
                result.add(session);
            }
        }

        return result;
    }

    // =====================================================
    // 🔹 CLEAR ALL SESSIONS
    // =====================================================

    @Override
    public void clearAllSessions(String userId) {

        sessions.entrySet().removeIf(
                entry -> entry.getValue().getUserId().equals(userId)
        );
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