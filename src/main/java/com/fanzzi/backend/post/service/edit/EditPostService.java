package com.fanzzi.backend.post.service.edit;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.dto.*;
import com.fanzzi.backend.post.enums.AttachmentType;
import com.fanzzi.backend.post.enums.ContentType;
import com.fanzzi.backend.post.enums.MonetizationType;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.model.PostAttachment;
import com.fanzzi.backend.post.model.PostStats;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.post.repository.PostStatsRepository;
import com.fanzzi.backend.post.service.feed.HydratedFeedCacheService;
import com.fanzzi.backend.post.service.mapping.PostResponseMapper;
import com.fanzzi.backend.post.service.validation.PostValidationService;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EditPostService {

    private final ChannelPostRepository repository;
    private final PostStatsRepository statsRepository;
    private final PostResponseMapper mapper;
    private final ApplicationEventPublisher publisher;
    private final PostValidationService validationService;
    private final HydratedFeedCacheService cacheService;

    private static final int MAX_ATTACHMENTS = 10;

    @Transactional
    public PostResponse editPost(String channelId, String postId, EditPostRequest request) {

        ChannelPost post = repository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));

        if (!post.getChannelId().equals(channelId)) {
            throw new ApiException(ErrorCode.INVALID_CHANNEL);
        }

        String userId = SecurityUtil.getCurrentUserId();

        if (!post.getPostedByUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        validationService.validateEditPost(post, request);

        Instant now = Instant.now();

        // =====================================
        // 🧾 EDIT HISTORY
        // =====================================
        PostEditHistory history = PostEditHistory.builder()
                .previousText(post.getText())
                .previousAttachments(post.getAttachments())
                .previousContentType(post.getContentType())
                .previousMonetizationType(post.getMonetizationType())
                .previousPrice(post.getPrice())
                .previousPreviewSeconds(post.getPreviewSeconds())
                .editedBy(userId)
                .editedAt(now)
                .build();

        if (post.getEditHistory() == null) {
            post.setEditHistory(new ArrayList<>());
        }
        post.getEditHistory().add(history);

        boolean changed = false;

        // =====================================
        // ✏️ TEXT
        // =====================================
        if (request.getText() != null) {
            String text = request.getText().trim();

            if (text.length() > 5000) {
                throw new ApiException(ErrorCode.INVALID_REQUEST, "Text too long");
            }

            if (!text.equals(post.getText())) {
                post.setText(text);
                changed = true;
            }
        }

        // =====================================
        // 📎 ATTACHMENTS
        // =====================================
        List<PostAttachment> current =
                post.getAttachments() != null
                        ? new ArrayList<>(post.getAttachments())
                        : new ArrayList<>();

        boolean mediaChanged = false;

        // REMOVE
        if (request.getRemoveAttachmentKeys() != null && !request.getRemoveAttachmentKeys().isEmpty()) {

            int beforeSize = current.size();

            current.removeIf(a -> request.getRemoveAttachmentKeys().contains(a.getKey()));

            if (current.size() != beforeSize) {
                mediaChanged = true;
            }
        }

        // ADD
        if (request.getAddAttachments() != null && !request.getAddAttachments().isEmpty()) {

            List<PostAttachment> additions = mapAttachments(request.getAddAttachments());

            additions.forEach(att -> {
                validationService.validateFileAttachment(att);

                if (!validationService.isFileOwnedByUser(att.getKey(), userId)) {
                    throw new ApiException(ErrorCode.FORBIDDEN, "Invalid file ownership");
                }
            });

            current.addAll(additions);
            mediaChanged = true;
        }

        // LIMIT CHECK
        if (current.size() > MAX_ATTACHMENTS) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Max 10 attachments allowed");
        }

        if (mediaChanged) {

            post.setAttachments(current);
            post.setAttachmentsCount(current.size());

            changed = true;

            // SAFE content type detection
            if (current.isEmpty()) {
                post.setContentType(ContentType.TEXT);
            } else {
                AttachmentType type = current.stream()
                        .findFirst()
                        .map(PostAttachment::getType)
                        .orElse(AttachmentType.IMAGE);

                post.setContentType(switch (type) {
                    case IMAGE -> ContentType.IMAGE;
                    case VIDEO -> ContentType.VIDEO;
                    case AUDIO -> ContentType.AUDIO;
                    case FILE -> ContentType.FILE;
                    default -> ContentType.TEXT;
                });
            }
        }

        // =====================================
        // 💰 MONETIZATION
        // =====================================
        if (request.getMonetizationType() != null) {

            MonetizationType newType = request.getMonetizationType();

            if (post.getContentType() == ContentType.FILE ||
                    post.getContentType() == ContentType.TEXT ||
                    post.getContentType() == ContentType.POLL) {

                newType = MonetizationType.FREE;
            }

            post.setMonetizationType(newType);

            if (newType == MonetizationType.PAID) {

                if (request.getPrice() == null || request.getPrice() <= 0) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid price");
                }

                post.setPrice(request.getPrice());

                int preview = request.getPreviewSeconds() != null ? request.getPreviewSeconds() : 0;

                if (preview < 0) {
                    throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid preview");
                }

                post.setPreviewSeconds(preview);

            } else {
                post.setPrice(0);
                post.setPreviewSeconds(0);
            }

            changed = true;
        }

        if (!changed) {
            throw new ApiException(ErrorCode.NO_CHANGES_DETECTED);
        }

        post.setEdited(true);
        post.setUpdatedAt(now);

        ChannelPost saved = repository.save(post);

        // =====================================
        // 🔄 MAP ONCE (IMPORTANT)
        // =====================================
        PostStats stats = statsRepository.findById(postId).orElse(null);
        long views = stats != null ? stats.getViews() : 0;



        PostResponse response = mapper.map(saved, views, userId);

        // =====================================
        // 🔥 CACHE UPDATE
        // =====================================
        try {
            cacheService.updatePost(saved.getChannelId(), response);
        } catch (Exception e) {
            log.warn("Cache update failed channelId={}", channelId, e);
        }

        // =====================================
        // 🚀 REALTIME EVENT
        // =====================================
        publisher.publishEvent(
                new PostEditRealtimeEvent(
                        saved.getChannelId(),
                        response
                )
        );

        return response;
    }

    private List<PostAttachment> mapAttachments(List<AttachmentRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        return requests.stream()
                .map(req -> PostAttachment.builder()
                        .key(req.getKey())
                        .type(req.getType())
                        .build())
                .toList();
    }
}