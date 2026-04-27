package com.fanzzi.backend.channel.report.dto.request;

/*
 * Reasons a user can select when reporting a channel.
 *
 * These values are used by:
 * - Mobile / frontend reporting UI
 * - Moderation pipeline
 * - Admin moderation dashboard
 * - Analytics & abuse detection
 */
public enum ReportReason {

    /*
     * Channel is sending unwanted promotional
     * messages or repetitive advertisements.
     */
    SPAM,

    /*
     * Channel is harassing, threatening,
     * or abusing users.
     */
    ABUSE,

    /*
     * Channel is impersonating someone
     * or pretending to be official.
     */
    FAKE,

    /*
     * Channel is involved in scams,
     * fraud, or phishing activities.
     */
    SCAM,

    /*
     * Channel contains adult or explicit
     * content not allowed on the platform.
     */
    NSFW,

    /*
     * Channel is distributing copyrighted
     * material without permission.
     */
    COPYRIGHT,

    /*
     * Any other reason not covered above.
     * Users may provide additional details
     * in the description field.
     */
    OTHER
}

