package com.fanzzi.backend.auth.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "auth_users")

@CompoundIndexes({
        @CompoundIndex(name = "phone_unique_idx", def = "{'phone': 1}", unique = true)
})
public class AuthUser {

    // =====================================================
    // 🆔 PRIMARY ID
    // =====================================================

    @Id
    private String id;

    // =====================================================
    // 📱 IDENTITY
    // =====================================================

    private String phone;         // normalized E.164
    private String countryCode;

    // =====================================================
    // 🔐 AUTHORIZATION
    // =====================================================

    private Role role = Role.USER;

    public enum Role {
        USER,
        ADMIN
    }

    // =====================================================
    // 🚦 ACCOUNT STATUS
    // =====================================================

    private boolean active = true;
    private boolean banned = false;
    private boolean deleted = false;

    private String banReason;
    private Instant bannedAt;

    // =====================================================
    // ⏱ AUDIT
    // =====================================================

    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
}