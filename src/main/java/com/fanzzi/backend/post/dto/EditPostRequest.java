package com.fanzzi.backend.post.dto;

import com.fanzzi.backend.post.enums.MonetizationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EditPostRequest {

    @Size(max = 2000)
    private String text;

    @Valid
    @Size(max = 10)
    private List<AttachmentRequest> addAttachments;

    @Size(max = 10)
    private List<
            @Pattern(regexp = "^[a-zA-Z0-9/_\\-.]+$")
                    String
            > removeAttachmentKeys;

    private MonetizationType monetizationType;

    @Min(0)
    private Integer price;

    @Min(0)
    @Max(10)
    private Integer previewSeconds;

    // =====================================
    // 🔥 HELPERS
    // =====================================

    public String getText() {
        return text != null ? text.trim() : null;
    }

    public void validateNoDuplicates() { /* as above */ }

    public void validateNoConflict() { /* as above */ }

    public void validateBusinessRules(boolean hasExistingContent) { /* as above */ }
}