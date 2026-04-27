package com.fanzzi.backend.post.service.poll;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.dto.Poll;
import com.fanzzi.backend.post.dto.PollOption;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.model.PollVote;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.repository.PollVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollService {

    private final PollVoteRepository pollVoteRepository;
    private final ChannelPostRepository postRepository;
    private final PollRedisService redisService;
    private final PollCacheService pollCacheService; // 🔥 NEW
    private final ApplicationEventPublisher publisher;

    @Transactional
    public Poll vote(String postId, String optionId, String userId) {

        if (postId == null || optionId == null || userId == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        ChannelPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));

        Poll poll = post.getPoll();

        if (poll == null) throw new ApiException(ErrorCode.NOT_FOUND, "Poll not found");
        if (poll.isClosed()) throw new ApiException(ErrorCode.FORBIDDEN, "Poll closed");

        if (poll.getExpiresAt() != null &&
                poll.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Poll expired");
        }

        boolean optionExists = poll.getOptions()
                .stream()
                .anyMatch(o -> o.getOptionId().equals(optionId));

        if (!optionExists) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid option");
        }

        List<PollVote> existingVotes =
                pollVoteRepository.findByPostIdAndUserId(postId, userId);

        boolean isQuiz = poll.isQuizMode();
        boolean isMultiple = poll.isMultipleChoice();

        try {

            // ================= QUIZ =================
            if (isQuiz) {

                if (!existingVotes.isEmpty()) {
                    throw new ApiException(ErrorCode.FORBIDDEN, "Already answered");
                }

                saveVote(postId, optionId, userId);
                redisService.increment(postId, optionId);
            }

            // ================= MULTIPLE =================
            else if (isMultiple) {

                boolean alreadySelected = existingVotes.stream()
                        .anyMatch(v -> v.getOptionId().equals(optionId));

                if (alreadySelected) {

                    if (!poll.isAllowVoteChange()) {
                        throw new ApiException(ErrorCode.FORBIDDEN, "Vote change not allowed");
                    }

                    existingVotes.stream()
                            .filter(v -> v.getOptionId().equals(optionId))
                            .findFirst()
                            .ifPresent(v -> {
                                pollVoteRepository.delete(v);
                                redisService.decrement(postId, optionId);
                            });

                } else {
                    saveVote(postId, optionId, userId);
                    redisService.increment(postId, optionId);
                }
            }

            // ================= SINGLE =================
            else {

                if (!existingVotes.isEmpty()) {

                    if (!poll.isAllowVoteChange()) {
                        throw new ApiException(ErrorCode.FORBIDDEN, "Already voted");
                    }

                    existingVotes.forEach(v -> {
                        redisService.decrement(postId, v.getOptionId());
                        pollVoteRepository.delete(v);
                    });
                }

                saveVote(postId, optionId, userId);
                redisService.increment(postId, optionId);
            }

        } catch (DuplicateKeyException e) {
            // 🔥 race condition protection
            throw new ApiException(ErrorCode.CONFLICT, "Already voted");
        }

        // =====================================
        // 🔥 MERGE DB + REDIS (FIXED)
        // =====================================
        Map<String, Long> redisStats = redisService.getStats(postId);

        long total = 0;

        for (PollOption opt : poll.getOptions()) {

            long base = opt.getVotes(); // DB
            long delta = redisStats.getOrDefault(opt.getOptionId(), 0L);

            long finalVotes = base + delta;

            if (finalVotes < 0) finalVotes = 0;

            opt.setVotes(finalVotes);
            total += finalVotes;
        }

        poll.setTotalVotes(total);

        // =====================================
        // 🔥 CACHE UPDATE
        // =====================================
        pollCacheService.put(postId, poll);

        // =====================================
        // 🚀 REALTIME
        // =====================================
        publisher.publishEvent(new PollVoteEvent(postId));

        return poll;
    }

    private void saveVote(String postId, String optionId, String userId) {

        PollVote vote = new PollVote();
        vote.setPostId(postId);
        vote.setOptionId(optionId);
        vote.setUserId(userId);
        vote.setCreatedAt(Instant.now());

        pollVoteRepository.save(vote);
    }
}