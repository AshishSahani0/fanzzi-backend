package com.fanzzi.backend.channel.report.dto.response;
import com.fanzzi.backend.channel.report.dto.request.ReportReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChannelReportResponse {

    // =========================================================
    // 🆔 REPORT INFO
    // =========================================================

    /*
     * Unique report identifier.
     * Used by admin moderation tools.
     */
    private String id;

    // =========================================================
    // 👤 REPORTER INFO
    // =========================================================

    /*
     * ID of the user who submitted the report.
     */
    private String reportedByUserId;

    /*
     * Display name of the reporting user.
     * Useful for moderation dashboards.
     */
    private String reportedByName;

    /*
     * Avatar/profile image of the reporter.
     */
    private String reportedByAvatar;

    /*
     * Indicates whether the reporter is a verified user.
     * Moderators can treat verified users as more trustworthy.
     */
    private boolean reporterVerified;

    // =========================================================
    // 🚩 REPORT DETAILS
    // =========================================================

    /*
     * Reason selected by the reporting user.
     */
    private ReportReason reason;

    /*
     * Optional description explaining the report.
     */
    private String description;

    /*
     * Optional evidence media key.
     * Stored as storage key (not full URL).
     * Backend generates signed URL when needed.
     */
    private String evidenceMediaKey;

    // =========================================================
    // ⭐ TRUST SYSTEM
    // =========================================================

    /*
     * Trust score assigned to the reporter.
     * Used by moderation algorithms to evaluate
     * the severity of reports.
     */
    private double reporterTrustScore;

    // =========================================================
    // ⏱ AUDIT
    // =========================================================

    /*
     * Timestamp when the report was submitted.
     */
    private Instant reportedAt;
}

