package com.fanzzi.backend.auth.device.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "user_devices")

@CompoundIndex(
        name = "user_device_idx",
        def = "{'userId': 1, 'deviceId': 1}",
        unique = true
)
public class UserDevice {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String deviceId;

    private String fcmToken;

    private String platform;
    private String deviceName;
    private String osVersion;
    private String appVersion;



    private String ipAddress;
    private String userAgent;
    private String fingerprint;

    private boolean trusted = true;
    private boolean blocked = false;
    private boolean suspicious = false;

    private Instant lastIpChangeAt;

    @Indexed
    private Instant lastActiveAt;

    private Instant createdAt = Instant.now();
}