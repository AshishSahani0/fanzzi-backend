package com.fanzzi.backend.channel.report.common.trust.service;

import com.fanzzi.backend.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class UserTrustService {

    // ======================================================
    // TRUST WEIGHTS
    // ======================================================

    /*
     * Trust score assigned to verified users.
     *
     * Verified users are considered more reliable
     * reporters, so their reports carry higher weight
     * in the moderation system.
     */
    private static final double VERIFIED_USER_WEIGHT = 2.0;

    /*
     * Default trust score for normal users whose
     * accounts are older than the minimum threshold.
     */
    private static final double NORMAL_USER_WEIGHT = 1.0;

    /*
     * Lower trust score assigned to new users.
     *
     * This prevents newly created accounts from
     * abusing the reporting system.
     */
    private static final double NEW_USER_WEIGHT = 0.5;

    /*
     * Minimum number of account age days required
     * before a user is treated as a normal trusted user.
     */
    private static final long TRUST_DAYS_THRESHOLD = 7;

    /*
     * Number of milliseconds in one day.
     * Used for fast account age calculation.
     */
    private static final long MILLIS_PER_DAY = 86_400_000L;

    // ======================================================
    // MAIN TRUST CALCULATION
    // ======================================================

    /*
     * Calculates the trust weight of a reporting user.
     *
     * This value contributes to the moderation risk score
     * of a reported channel.
     *
     * Example moderation formula:
     *
     * channelRiskScore =
     *     sum(reporterTrustScore)
     *     + numberOfReports
     *
     * Example weights:
     * Verified user → 2.0
     * Normal user   → 1.0
     * New user      → 0.5
     */
    public double calculateReportWeight(User user) {

        if (user == null) {
            log.warn("Trust calculation received null user");
            return NEW_USER_WEIGHT;
        }

        // Verified users always receive the highest trust score
        if (Boolean.TRUE.equals(user.isVerified())) {
            return VERIFIED_USER_WEIGHT;
        }

        Instant createdAt = user.getCreatedAt();

        if (createdAt == null) {
            log.debug("User {} has no createdAt timestamp", user.getId());
            return NEW_USER_WEIGHT;
        }

        long accountAgeDays = getAccountAgeDays(createdAt);

        // Older accounts are treated as normal trusted users
        return accountAgeDays >= TRUST_DAYS_THRESHOLD
                ? NORMAL_USER_WEIGHT
                : NEW_USER_WEIGHT;
    }

    // ======================================================
    // HELPER METHODS
    // ======================================================

    /*
     * Calculates account age in days.
     *
     * Uses epoch milliseconds for faster calculation
     * without creating additional objects.
     */
    private long getAccountAgeDays(Instant createdAt) {

        long createdMillis = createdAt.toEpochMilli();
        long nowMillis = System.currentTimeMillis();

        // Prevent invalid timestamps
        if (createdMillis <= 0 || createdMillis > nowMillis) {
            log.warn("Invalid createdAt timestamp detected: {}", createdAt);
            return 0;
        }

        return (nowMillis - createdMillis) / MILLIS_PER_DAY;
    }
}

