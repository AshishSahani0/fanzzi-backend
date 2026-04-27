package com.fanzzi.backend.channel.report.dto.request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportRequest {

    /*
     * Reason selected by the user when reporting the channel.
     *
     * Example values:
     * SPAM
     * SCAM
     * HARASSMENT
     * ILLEGAL_CONTENT
     */
    @NotNull(message = "Reason is required")
    private ReportReason reason;

    /*
     * Optional explanation from the user.
     *
     * Used by moderators to understand the context
     * of the report.
     *
     * Max length limited to prevent abuse.
     */
    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    /*
     * Optional evidence uploaded by the user.
     *
     * IMPORTANT:
     * This stores only the storage key of the media
     * (not the public URL).
     *
     * Example:
     * reports/evidence/abc123.png
     *
     * The backend will generate a signed URL when
     * moderators need to view the evidence.
     */
    private String evidenceMediaKey;
}

