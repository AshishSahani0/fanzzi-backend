package com.fanzzi.backend.post.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Poll {

    private String question;

    private List<PollOption> options;

    private boolean multipleChoice;

    private boolean quizMode;

    private String correctOptionId; // ⚠️ hide conditionally

    private boolean allowVoteChange;

    private long totalVotes;

    @Builder.Default
    private List<String> userSelectedOptionIds = List.of();

    private Instant expiresAt;

    private boolean closed;

    // =====================================
    // 🔥 HELPERS
    // =====================================

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean hasUserVoted() {
        return userSelectedOptionIds != null && !userSelectedOptionIds.isEmpty();
    }

    public boolean isActive() {
        return !closed && !isExpired();
    }
}