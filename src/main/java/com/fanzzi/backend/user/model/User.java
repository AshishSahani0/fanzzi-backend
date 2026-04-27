package com.fanzzi.backend.user.model;

import com.fanzzi.backend.user.common.Role;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Document(collection = "users")

@CompoundIndexes({

        // Fast username lookup
        @CompoundIndex(name = "username_idx",
                def = "{'userNameLower': 1}",
                unique = true,
                sparse = true),

        // Phone lookup
        @CompoundIndex(name = "phone_idx",
                def = "{'phone': 1}",
                unique = true),

        // Search optimization
        @CompoundIndex(name = "search_idx",
                def = "{'firstName': 1, 'lastName': 1}")
})
public class User {

    @Id
    private String id;

    // ============================
    // 🔐 IDENTITY
    // ============================

    @Indexed(unique = true)
    private String phone;

    private String countryCode;

    @Indexed
    private String userName;

    // Lowercase copy for fast case-insensitive search
    private String userNameLower;

    private String email;
    private boolean emailVerified = false;

    // ============================
    // 👤 PROFILE
    // ============================

    private String firstName;
    private String lastName;
    private String bio;

    private LocalDate dateOfBirth;
    private String profileImageKey;

    // ============================
    // 🛡 ACCOUNT STATUS
    // ============================

    @Indexed
    private boolean active = true;

    @Indexed
    private boolean banned = false;

    private String banReason;
    private Instant bannedAt;

    @Indexed
    private boolean deleted = false;

    private Instant deletedAt;

    // ============================
    // ⭐ TRUST & VERIFICATION
    // ============================

    private boolean verified = false;
    private boolean hidePhone = false;

    // ============================
    // 🔑 SYSTEM
    // ============================

    private Role role = Role.USER;

    @Indexed
    private Instant createdAt = Instant.now();

    private Instant updatedAt;
    private Instant lastLoginAt;
}