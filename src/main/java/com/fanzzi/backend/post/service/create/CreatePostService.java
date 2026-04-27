package com.fanzzi.backend.post.service.create;

import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.dto.*;
import com.fanzzi.backend.post.enums.*;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.model.NewPostEvent;
import com.fanzzi.backend.post.model.PostAttachment;
import com.fanzzi.backend.post.model.PostStats;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.repository.PostStatsRepository;
import com.fanzzi.backend.post.service.feed.HydratedFeedCacheService;
import com.fanzzi.backend.post.service.mapping.PostResponseMapper;
import com.fanzzi.backend.post.service.sequence.ChannelPostSequenceService;
import com.fanzzi.backend.post.service.validation.PostValidationService;
import com.fanzzi.backend.post.util.FeedBucketUtil;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreatePostService {

    private final ChannelRepository channelRepository;
    private final ChannelPostRepository postRepository;
    private final PostStatsRepository statsRepository;
    private final PostValidationService validationService;
    private final ChannelPostSequenceService sequenceService;
    private final PostResponseMapper mapper;
    private final ApplicationEventPublisher publisher;
    private final HydratedFeedCacheService cacheService;


    public PostResponse createPost(String channelId, CreatePostRequest request) {


        validationService.validateCreatePost(request);

        // EMPTY POST CHECK
        boolean noText = request.getText() == null || request.getText().isBlank();
        boolean noAttachments = request.getAttachments() == null || request.getAttachments().isEmpty();
        boolean noPoll = request.getPoll() == null;

        if (noText && noAttachments && noPoll) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Post cannot be empty");
        }


        //  ATTACHMENT LIMIT

        if (request.getAttachments() != null && request.getAttachments().size() > 10) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Max 10 attachments allowed");
        }

        String userId = SecurityUtil.getCurrentUserId();

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHANNEL_NOT_FOUND));

        if (!channel.getOwnerId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        // ==========================================
        // SEQUENCE
        // ==========================================
        long seq = sequenceService.nextSeq(channelId);
        int bucketId = Math.toIntExact(FeedBucketUtil.calculateBucket(seq));
        Instant now = Instant.now();

        // ==========================================
        // ATTACHMENTS
        // ==========================================
        List<PostAttachment> attachments = mapAttachments(request.getAttachments());

        attachments.forEach(att -> {
            validationService.validateFileAttachment(att);


            if (!validationService.isFileOwnedByUser(att.getKey(), userId)) {
                throw new ApiException(ErrorCode.FORBIDDEN, "Invalid file ownership");
            }
        });

        boolean hasAttachments = !attachments.isEmpty();
        ContentType contentType = getContentType(request, hasAttachments, attachments);


        // POLL + TEXT RULE

        if (request.getPoll() != null &&
                request.getText() != null &&
                !request.getText().isBlank()) {

            throw new ApiException(ErrorCode.INVALID_REQUEST, "Poll cannot have text");
        }

        // ==========================================
        // 🗳️ POLL
        // ==========================================
        Poll poll = null;

        if (contentType == ContentType.POLL) {

            if (hasAttachments) {
                throw new ApiException(ErrorCode.INVALID_REQUEST);
            }

            poll = mapPoll(request.getPoll());
        }

        boolean downloadable =
                request.getDownloadable() != null && request.getDownloadable();

        // ==========================================
        // 💰 MONETIZATION LOGIC
        // ==========================================
        MonetizationType monetizationType = request.getMonetizationType();

        // ❌ FORCE FREE CASES
        if (contentType == ContentType.FILE ||
                contentType == ContentType.TEXT ||
                contentType == ContentType.POLL) {

            monetizationType = MonetizationType.FREE;
        }

        boolean isPaid = monetizationType == MonetizationType.PAID;

        long price = 0;
        int preview = 0;

        if (isPaid) {

            if (request.getPrice() <= 0) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Price must be greater than 0");
            }

            if (request.getPreviewSeconds() < 0) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid preview duration");
            }

            price = request.getPrice();
            preview = request.getPreviewSeconds();
        }

        // ==========================================
        // CREATE POST
        // ==========================================
        ChannelPost post = ChannelPost.builder()
                .channelId(channelId)
                .seq(seq)
                .bucketId(bucketId)
                .postedByUserId(userId)
                .text(request.getText() != null ? request.getText().trim() : null)
                .attachments(attachments)
                .attachmentsCount(hasAttachments ? attachments.size() : 0)
                .contentType(contentType)
                .monetizationType(monetizationType)
                .price(price)
                .previewSeconds(preview)
                .poll(poll)
                .downloadable(downloadable)
                .createdAt(now)
                .updatedAt(now)
                .status(PostStatus.PUBLISHED)
                .deleted(false)
                .build();

        ChannelPost saved = postRepository.save(post);

        // ==========================================
        // STATS
        // ==========================================
        PostStats stats = new PostStats();
        stats.setPostId(saved.getId());
        stats.setViews(0);
        stats.setShares(0);
        stats.setComments(0);
        stats.setReactions(0);
        stats.setUpdatedAt(now);

        statsRepository.save(stats);

        CompletableFuture.runAsync(() -> {
            try {
                retryIncrementPostStats(channelId, now);
            } catch (Exception ignored) {}
        });


        PostResponse response = mapper.map(saved, 0, userId);


        // 🔥 SAFE CACHE WRITE (FAIL-SAFE)

        try {
            cacheService.cachePost(channelId, response);
        } catch (Exception e) {

            log.warn("Feed cache failed for channelId={} error={}", channelId, e.getMessage());
        }

// ==========================================
// 🚀 EVENT (REALTIME ONLY)
// ==========================================
        publisher.publishEvent(
                NewPostEvent.builder()
                        .type(EventType.POST_CREATED)
                        .channelId(channelId)
                        .postId(saved.getId())
                        .seq(saved.getSeq())
                        .createdAt(now)
                        .hasMedia(!attachments.isEmpty())
                        .isPaid(isPaid)
                        .build()
        );

        return response;

    }

    private void retryIncrementPostStats(String channelId, Instant now) {

        int retries = 5;

        for (int i = 0; i < retries; i++) {
            try {
                channelRepository.incrementPostStats(channelId, now);
                return;
            } catch (Exception e) {

                if (i == retries - 1) {
                    throw e;
                }

                try {
                    Thread.sleep(50L * (i + 1)); // 🔥 backoff
                } catch (InterruptedException ignored) {}
            }
        }
    }

    @NonNull
    private static ContentType getContentType(CreatePostRequest request, boolean hasAttachments, List<PostAttachment> attachments) {
        boolean hasText = request.getText() != null && !request.getText().isBlank();
        boolean hasPoll = request.getPoll() != null;

        // ==========================================
        // DETECT CONTENT TYPE
        // ==========================================
        ContentType contentType;

        if (hasPoll) {
            contentType = ContentType.POLL;
        } else if (!hasAttachments && hasText) {
            contentType = ContentType.TEXT;
        } else if (hasAttachments) {
            AttachmentType type = attachments.get(0).getType();

            contentType = switch (type) {
                case IMAGE -> ContentType.IMAGE;
                case VIDEO -> ContentType.VIDEO;
                case AUDIO -> ContentType.AUDIO;
                case FILE -> ContentType.FILE;
                default -> ContentType.TEXT;
            };
        } else {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }
        return contentType;
    }

    // ======================================================
    // ATTACHMENT MAPPER
    // ======================================================
    private List<PostAttachment> mapAttachments(List<AttachmentRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        return requests.stream()
                .map(req -> PostAttachment.builder()
                        .key(req.getKey())
                        .type(req.getType())
                        .fileName(req.getFileName())
                        .fileSize(req.getFileSize())
                        .mimeType(req.getMimeType())
                        .build())
                .toList();
    }

    // ======================================================
    // POLL MAPPER
    // ======================================================
    private Poll mapPoll(CreatePollRequest request) {

        if (request == null) return null;

        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getOptions() == null || request.getOptions().size() < 2) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        List<PollOption> options = request.getOptions().stream()
                .map(opt -> PollOption.builder()
                        .optionId(UUID.randomUUID().toString())
                        .text(opt)
                        .votes(0)
                        .build())
                .toList();

        String correctOptionId = null;

        boolean isQuiz = Boolean.TRUE.equals(request.isQuizMode());
        Integer correctIndex = request.getCorrectOptionIndex();

        if (isQuiz) {
            if (correctIndex == null ||
                    correctIndex < 0 ||
                    correctIndex >= options.size()) {
                throw new ApiException(ErrorCode.INVALID_REQUEST);
            }

            correctOptionId = options.get(correctIndex).getOptionId();
        }

        return Poll.builder()
                .question(request.getQuestion())
                .options(options)
                .multipleChoice(request.isMultipleChoice())
                .quizMode(isQuiz)
                .correctOptionId(correctOptionId)
                .allowVoteChange(request.isAllowVoteChange())
                .totalVotes(0)
                .expiresAt(request.getExpiresAt())
                .closed(false)
                .build();
    }
}