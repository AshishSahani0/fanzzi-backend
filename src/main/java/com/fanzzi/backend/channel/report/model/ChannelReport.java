package com.fanzzi.backend.channel.report.model;

import com.fanzzi.backend.channel.report.dto.ModerationAction;
import com.fanzzi.backend.channel.report.dto.ReportStatus;
import com.fanzzi.backend.channel.report.dto.request.ReportReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "channel_reports")

/*
 * Prevent duplicate reports from the same user for the same channel.
 * Example:
 * userA cannot report channelX twice.
 */
@CompoundIndex(
        name = "channel_user_unique",
        def = "{'channelId':1,'reportedBy':1}",
        unique = true
)

/*
 * Optimizes queries when retrieving reports of a channel
 * ordered by latest reports first.
 */
@CompoundIndex(
        name = "channel_reports_lookup",
        def = "{'channelId':1,'reportedAt':-1}"
)

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelReport {

    @Id
    private String id;

    // =====================================================
    // TARGET CHANNEL
    // =====================================================

    /*
     * ID of the channel being reported.
     *
     * Used by:
     * - User (while reporting a channel)
     * - Moderation system (to evaluate reports)
     * - Admin dashboard (to review abuse)
     */
    @Indexed
    private String channelId;

    /*
     * ID of the user who submitted the report.
     *
     * Used for:
     * - preventing duplicate reports
     * - calculating trust score
     * - investigation by moderators
     */
    @Indexed
    private String reportedBy;

    // =====================================================
    // REPORT INFORMATION (USER INPUT)
    // =====================================================

    /*
     * Category selected by user while reporting.
     *
     * Example:
     * SPAM
     * SCAM
     * HARASSMENT
     * ILLEGAL_CONTENT
     */
    private ReportReason reason;

    /*
     * Optional explanation provided by the user.
     * Sanitized in service layer to prevent abuse.
     */
    private String description;

    /*
     * Optional evidence uploaded by the user.
     *
     * IMPORTANT:
     * Only the storage key is stored (NOT full URL).
     *
     * Example:
     * reports/evidence/abc123.png
     *
     * Backend generates a signed URL when admins
     * or moderators need to view the file.
     */
    private String evidenceMediaKey;

    // =====================================================
    // TRUST SYSTEM (SYSTEM GENERATED)
    // =====================================================

    /*
     * Trust score of the reporting user.
     *
     * Calculated using UserTrustService based on:
     * - verified account
     * - account age
     * - reputation
     *
     * Example:
     * Verified user → 2.0
     * Normal user   → 1.0
     * New user      → 0.5
     *
     * This value contributes to the channel's
     * overall moderation risk score.
     */
    private double reporterTrustScore;

    // =====================================================
    // AUTOMATED MODERATION PIPELINE (SYSTEM)
    // =====================================================

    /*
     * Current processing state of the report.
     *
     * Example:
     * PENDING   → waiting for moderation
     * REVIEWED  → admin checked
     * DISMISSED → report invalid
     */
    @Indexed
    private ReportStatus status;

    /*
     * Action taken by the moderation system or admin.
     *
     * Automatic moderation pipeline may trigger:
     *
     * AUTO_WARNING
     * AUTO_HIDE_CHANNEL
     * AUTO_BAN_CHANNEL
     *
     * These decisions are based on:
     *
     * totalTrustScore(channel)
     * +
     * numberOfReports(channel)
     *
     * Example moderation thresholds:
     *
     * trustScore >= 5   → warning
     * trustScore >= 10  → auto hide
     * trustScore >= 20  → auto ban
     */
    private ModerationAction actionTaken;

    /*
     * Moderator or admin who reviewed the report.
     * Null if handled automatically by system.
     */
    private String reviewedBy;

    /*
     * Timestamp when moderation action occurred.
     */
    private Instant reviewedAt;

    // =====================================================
    // AUDIT INFORMATION
    // =====================================================

    /*
     * Time when the report was submitted.
     *
     * Automatically populated by Spring Data.
     * Used for:
     * - moderation queues
     * - analytics
     * - report timelines
     */
    @CreatedDate
    private Instant reportedAt;
}