package com.fanzzi.backend.channel.dto.request;

import com.fanzzi.backend.channel.enums.ChannelType;
import com.fanzzi.backend.channel.enums.ChannelVisibility;
import jakarta.validation.constraints.*;

import lombok.Data;

/**
 * =====================================================
 * 📥 UPDATE CHANNEL REQUEST
 * =====================================================
 * Owner-only settings update.
 *
 * All fields are OPTIONAL.
 * Only provided fields will be updated.
 */
@Data
public class UpdateChannelRequest {

    // =====================================================
    // 📝 BASIC INFORMATION
    // =====================================================

    /**
     * Channel display name
     */
    @Size(min = 3, max = 100,
            message = "Name must be 3–100 characters")
    private String name;

    /**
     * Channel description/about
     */
    @Size(max = 500,
            message = "Description too long")
    private String description;

    /**
     * Storage key for profile image
     */
    private String profileImageKey;

    // =====================================================
    // 🔐 ACCESS SETTINGS
    // =====================================================

    /**
     * PUBLIC / PRIVATE
     */
    private ChannelVisibility visibility;

    /**
     * FREE / PAID
     */
    private ChannelType type;

    /**
     * Monthly subscription price
     * Required only when type = PAID
     */
    @Positive(message = "Price must be positive")
    @Max(value = 100000,
            message = "Price too high")
    private Long monthlyPrice;

    // =====================================================
    // 🌍 DISCOVERY & CLASSIFICATION
    // =====================================================

    /**
     * Category (Tech, Gaming, etc.)
     */
    private String category;

    /**
     * Content language (en, hi, etc.)
     */
    private String language;

    /**
     * Target region (optional)
     */
    private String region;

    /**
     * Show channel in explore/search
     */
    private Boolean discoverable;

    // =====================================================
    // 🔞 SAFETY SETTINGS
    // =====================================================

    /**
     * Mark channel as NSFW
     */
    private Boolean nsfw;

    // =====================================================
    // 💬 INTERACTION SETTINGS
    // =====================================================

    /**
     * Require approval before joining
     */
    private Boolean joinApprovalRequired;

    /**
     * Allow members to comment on posts
     */
    private Boolean allowComments;
}