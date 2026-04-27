package com.fanzzi.backend.post.dto;

import com.fanzzi.backend.post.enums.MonetizationType;
import jakarta.validation.Valid;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.util.List;

@Data
public class CreatePostRequest {

    @Size(max = 2000, message = "Text cannot exceed 2000 characters")
    private String text;

    @Valid
    @Size(max = 10, message = "Maximum 10 attachments allowed")
    private List<AttachmentRequest> attachments;

    @Valid
    private CreatePollRequest poll;

    @NotNull(message = "Monetization type is required")
    private MonetizationType monetizationType;

    @NotNull(message = "Downloadable flag required")
    private Boolean downloadable;

    @Min(value = 0, message = "Price cannot be negative")
    private long price;

    @Min(value = 0, message = "Preview cannot be negative")
    @Max(value = 10, message = "Preview cannot exceed 10 seconds")
    private int previewSeconds;

    // =====================================
    // 🔥 BUSINESS VALIDATION
    // =====================================

    public boolean isEmpty() {
        return (text == null || text.isBlank())
                && (attachments == null || attachments.isEmpty())
                && poll == null;
    }

    public boolean hasConflict() {
        return poll != null && attachments != null && !attachments.isEmpty();
    }

    public void validateBusinessRules() {

        if (isEmpty()) {
            throw new IllegalArgumentException("Post cannot be empty");
        }

        if (hasConflict()) {
            throw new IllegalArgumentException("Poll cannot have attachments");
        }

        if (monetizationType == MonetizationType.PAID) {

            if (price <= 0) {
                throw new IllegalArgumentException("Paid post must have price > 0");
            }

            if (attachments == null || attachments.isEmpty()) {
                throw new IllegalArgumentException("Paid post requires media");
            }
        }
    }
}