package com.fanzzi.backend.auth.session.service;

import com.fanzzi.backend.auth.model.AuthUser;
import com.fanzzi.backend.auth.session.dto.UserSessionDTO;

import java.util.List;

public interface SessionService {

    UserSessionDTO getSession(String userId, String deviceId);

    UserSessionDTO saveSession(
            AuthUser user,
            String deviceId,
            String ipAddress,
            String userAgent
    );

    void clearSession(String userId, String deviceId);

    List<UserSessionDTO> getAllSessions(String userId);

    void clearAllSessions(String userId);
}