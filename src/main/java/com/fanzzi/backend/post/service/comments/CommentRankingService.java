package com.fanzzi.backend.post.service.comments;

import com.fanzzi.backend.post.model.PostComment;
import org.springframework.stereotype.Service;

@Service
public class CommentRankingService {

    // =====================================
    // 🔧 TUNABLE WEIGHTS
    // =====================================
    private static final double LIKE_WEIGHT = 3.0;
    private static final double REPLY_WEIGHT = 2.0;

    private static final double GRAVITY = 1.8;
    private static final double PIN_BOOST = 1000.0;   // 🔥 pinned priority
    private static final double FRESH_BOOST = 1.2;    // 🔥 new comments boost

    // =====================================
    // 🔥 MAIN RANKING
    // =====================================
    public double calculateScore(PostComment comment) {

        if (comment == null || comment.getCreatedAt() == null) {
            return 0;
        }

        // ===============================
        // SAFE VALUES
        // ===============================
        long likes = Math.max(0, comment.getLikes());
        long replies = Math.max(0, comment.getReplyCount());

        long ageSeconds = Math.max(
                1,
                (System.currentTimeMillis() -
                        comment.getCreatedAt().toEpochMilli()) / 1000
        );

        // ===============================
        // ENGAGEMENT
        // ===============================
        double engagement =
                (likes * LIKE_WEIGHT) +
                        (replies * REPLY_WEIGHT);

        // ===============================
        // TIME DECAY (Reddit-style)
        // ===============================
        double decay = Math.pow(ageSeconds / 3600.0 + 2, GRAVITY);

        double score = engagement / decay;

        // ===============================
        // 🔥 PIN BOOST (TOP ALWAYS)
        // ===============================
        if (comment.isPinned()) {
            score += PIN_BOOST;
        }

        // ===============================
        // 🔥 FRESH BOOST (FIRST 10 MIN)
        // ===============================
        if (ageSeconds < 600) { // 10 minutes
            score *= FRESH_BOOST;
        }

        // ===============================
        // 🛡️ STABILITY (tie breaker)
        // ===============================
        score += comment.getCreatedAt().toEpochMilli() * 1e-12;

        return score;
    }
}