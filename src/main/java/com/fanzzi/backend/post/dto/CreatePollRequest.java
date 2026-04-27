package com.fanzzi.backend.post.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreatePollRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 200, message = "Question too long")
    private String question;

    @NotNull(message = "Options required")
    @Size(min = 2, max = 6, message = "Poll must have 2 to 6 options")
    private List<@NotBlank(message = "Option cannot be empty") String> options;

    private boolean multipleChoice;

    private boolean quizMode;

    @Min(value = 0, message = "Invalid correct option index")
    private Integer correctOptionIndex;

    private boolean allowVoteChange;

    @Future(message = "Expiry must be in future")
    private Instant expiresAt;

    // =====================================
    // 🔥 VALIDATION LOGIC
    // =====================================
    public void validate() {

        if (quizMode) {
            if (correctOptionIndex == null) {
                throw new IllegalArgumentException("Correct option required for quiz");
            }

            if (options == null || correctOptionIndex >= options.size()) {
                throw new IllegalArgumentException("Correct option index invalid");
            }
        }
    }
}