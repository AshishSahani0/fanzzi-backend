package com.fanzzi.backend.auth.device.service;

import com.fanzzi.backend.auth.device.model.UserDevice;
import com.fanzzi.backend.auth.device.repository.UserDeviceRepository;
import com.fanzzi.backend.auth.refresh.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDeviceService {

    private final UserDeviceRepository repo;

    // =====================================================
    //  REGISTER / UPDATE DEVICE
    // =====================================================

    public UserDevice registerDevice(
            String userId,
            String deviceId,
            String fcmToken,
            String platform,
            String deviceName,
            String osVersion,
            String appVersion,
            String ipAddress,
            String userAgent
    ) {

        UserDevice device = repo
                .findByUserIdAndDeviceId(userId, deviceId)
                .orElseGet(UserDevice::new);

        boolean isNew = device.getId() == null;

        String safeIp = safe(ipAddress);
        String safeAgent = safe(userAgent);

        String newFingerprint = fingerprint(safeIp, safeAgent);

        // =====================================================
        // DETECT IP CHANGE
        // =====================================================

        if (!isNew && device.getIpAddress() != null &&
                !device.getIpAddress().equals(safeIp)) {

            device.setSuspicious(true);
            device.setLastIpChangeAt(Instant.now());
        }

        // =====================================================
        // SET FIELDS (SAFE UPDATE)
        // =====================================================

        device.setUserId(userId);
        device.setDeviceId(deviceId);

        if (notBlank(fcmToken)) device.setFcmToken(fcmToken);

        device.setPlatform(safe(platform, "ANDROID"));
        device.setDeviceName(safe(deviceName, "unknown-device"));
        device.setOsVersion(safe(osVersion, "unknown-os"));
        device.setAppVersion(safe(appVersion, "1.0.0"));

        device.setIpAddress(safeIp);
        device.setUserAgent(safeAgent);
        device.setFingerprint(newFingerprint);

        device.setLastActiveAt(Instant.now());

        if (isNew) {
            device.setCreatedAt(Instant.now());
        }

        return repo.save(device);
    }

    // =====================================================
    // GET USER DEVICES
    // =====================================================

    public List<UserDevice> getUserDevices(String userId) {
        return repo.findByUserId(userId);
    }

    // =====================================================
    // REMOVE DEVICE
    // =====================================================

    public void removeDevice(String userId, String deviceId) {
        repo.deleteByUserIdAndDeviceId(userId, deviceId);
    }

    // =====================================================
    // 🔐 HELPERS
    // =====================================================

    private String fingerprint(String ip, String agent) {
        return TokenHashUtil.sha256(ip + "|" + agent);
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "unknown" : value.trim();
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}