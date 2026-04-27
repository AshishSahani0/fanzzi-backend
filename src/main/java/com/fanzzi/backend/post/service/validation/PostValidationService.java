package com.fanzzi.backend.post.service.validation;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.post.dto.CreatePollRequest;
import com.fanzzi.backend.post.dto.CreatePostRequest;
import com.fanzzi.backend.post.dto.EditPostRequest;
import com.fanzzi.backend.post.enums.AttachmentType;
import com.fanzzi.backend.post.enums.ContentType;
import com.fanzzi.backend.post.enums.MonetizationType;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.model.PostAttachment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class PostValidationService {

    private static final int MAX_ATTACHMENTS = 10;
    private static final int MAX_TEXT_LENGTH = 2000;
    private static final int MAX_PREVIEW_SECONDS = 10;
    private static final int EDIT_WINDOW_SECONDS = 86400;

    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 12;

    private static final List<String> ALLOWED_DOC_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/zip",
            "application/x-rar-compressed"
    );

    private static final List<String> ALLOWED_AUDIO_TYPES = List.of(
            "audio/mpeg",
            "audio/mp3",
            "audio/wav",
            "audio/x-wav"
    );

    // ==========================================================
    // CREATE POST VALIDATION
    // ==========================================================

    public void validateCreatePost(CreatePostRequest request) {

        boolean hasText =
                request.getText() != null &&
                        !request.getText().isBlank();

        boolean hasAttachments =
                request.getAttachments() != null &&
                        !request.getAttachments().isEmpty();

        if (!hasText && !hasAttachments) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Post must contain text or media");
        }

        if (hasText && request.getText().length() > MAX_TEXT_LENGTH) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Post text too long");
        }

        // ❌ Poll + attachments not allowed
        if (request.getPoll() != null && hasAttachments) {
            throw new ApiException(ErrorCode.INVALID_POST);
        }

        if (hasAttachments && request.getAttachments().size() > MAX_ATTACHMENTS) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Maximum attachments allowed: " + MAX_ATTACHMENTS);
        }

        // ✅ Validate attachment types
        validateAttachmentRequests(request);

        // 🔥 APPLY RULES
        enforceContentRules(request, hasAttachments);

        // ✅ Paid rules
        validatePaidPost(request);
    }

    public boolean isFileOwnedByUser(String key, String userId) {
        if (key == null || userId == null) return false;


        return key.startsWith("user/" + userId + "/")
                || key.startsWith("channel/")
                || key.startsWith("public/");
    }

    // ==========================================================
    // 🔥 BUSINESS RULES
    // ==========================================================

    private void enforceContentRules(CreatePostRequest request, boolean hasAttachments) {

        boolean hasPoll = request.getPoll() != null;

        boolean hasFile = hasAttachments &&
                request.getAttachments().stream()
                        .anyMatch(a -> a.getType() == AttachmentType.FILE);

        boolean hasMedia = hasAttachments &&
                request.getAttachments().stream()
                        .anyMatch(a ->
                                a.getType() == AttachmentType.IMAGE ||
                                        a.getType() == AttachmentType.VIDEO ||
                                        a.getType() == AttachmentType.AUDIO
                        );

        // 🗳️ POLL → always FREE
        if (hasPoll) {
            request.setMonetizationType(MonetizationType.FREE);
            return;
        }

        // 📝 TEXT → always FREE
        if (!hasAttachments) {
            request.setMonetizationType(MonetizationType.FREE);
            return;
        }

        // 📄 FILE → always FREE
        if (hasFile) {
            request.setMonetizationType(MonetizationType.FREE);
            return;
        }

        // 🎬 MEDIA → allowed FREE / PAID (no override)
    }

    // ==========================================================
    // ATTACHMENT VALIDATION
    // ==========================================================

    private void validateAttachmentRequests(CreatePostRequest request) {

        if (request.getAttachments() == null) return;

        request.getAttachments().forEach(att -> {

            if (att.getType() == AttachmentType.FILE) {

                if (att.getMimeType() == null ||
                        !ALLOWED_DOC_TYPES.contains(att.getMimeType())) {
                    throw new ApiException(ErrorCode.BAD_REQUEST,
                            "Invalid file type");
                }

                if (att.getFileSize() <= 0) {
                    throw new ApiException(ErrorCode.BAD_REQUEST,
                            "Invalid file size");
                }
            }

            if (att.getType() == AttachmentType.AUDIO) {

                if (att.getMimeType() == null ||
                        !ALLOWED_AUDIO_TYPES.contains(att.getMimeType())) {
                    throw new ApiException(ErrorCode.BAD_REQUEST,
                            "Invalid audio type");
                }

                if (att.getFileSize() <= 0) {
                    throw new ApiException(ErrorCode.BAD_REQUEST,
                            "Invalid audio size");
                }
            }
        });
    }

    // ==========================================================
    // SAFE VALIDATION
    // ==========================================================

    public void validateFileAttachment(PostAttachment attachment) {

        if (attachment == null) return;

        if (attachment.getType() == AttachmentType.FILE) {

            if (attachment.getMimeType() == null ||
                    !ALLOWED_DOC_TYPES.contains(attachment.getMimeType())) {
                throw new ApiException(ErrorCode.INVALID_REQUEST);
            }

            if (attachment.getFileSize() <= 0) {
                throw new ApiException(ErrorCode.INVALID_REQUEST);
            }
        }
    }

    // ==========================================================
    // PAID RULES
    // ==========================================================

    private void validatePaidPost(CreatePostRequest request) {

        if (request.getMonetizationType() != MonetizationType.PAID) {
            return;
        }

        boolean hasValidMedia =
                request.getAttachments() != null &&
                        request.getAttachments().stream()
                                .anyMatch(att ->
                                        att.getType() == AttachmentType.IMAGE ||
                                                att.getType() == AttachmentType.VIDEO ||
                                                att.getType() == AttachmentType.AUDIO
                                );

        if (!hasValidMedia) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Paid posts must contain image, video, or audio");
        }

        if (request.getPrice() <= 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Paid post must have valid price");
        }

        if (request.getPreviewSeconds() < 0 ||
                request.getPreviewSeconds() > MAX_PREVIEW_SECONDS) {

            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Preview seconds must be between 0 and " + MAX_PREVIEW_SECONDS);
        }
    }

    // ==========================================================
    // EDIT VALIDATION
    // ==========================================================

    public void validateEditPost(ChannelPost post, EditPostRequest request) {

        if (post.isDeleted()) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }

        if (post.getCreatedAt()
                .plusSeconds(EDIT_WINDOW_SECONDS)
                .isBefore(Instant.now())) {

            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Editing allowed only within 24 hours");
        }

        int currentCount =
                post.getAttachments() != null ? post.getAttachments().size() : 0;

        int removeCount =
                request.getRemoveAttachmentKeys() != null
                        ? request.getRemoveAttachmentKeys().size()
                        : 0;

        int addCount =
                request.getAddAttachments() != null
                        ? request.getAddAttachments().size()
                        : 0;

        int finalCount = currentCount - removeCount + addCount;

        if (finalCount > MAX_ATTACHMENTS) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Maximum " + MAX_ATTACHMENTS + " attachments allowed");
        }

        if (finalCount < 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Invalid attachment removal");
        }

        // ❌ Paid edit lock
        if (post.getMonetizationType() == MonetizationType.PAID &&
                post.getPrice() > 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Paid posts cannot be edited");
        }

        if (request.getPrice() != null && request.getPrice() < 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Price cannot be negative");
        }

        if (request.getPreviewSeconds() != null &&
                (request.getPreviewSeconds() < 0 ||
                        request.getPreviewSeconds() > MAX_PREVIEW_SECONDS)) {

            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Invalid preview seconds");
        }
    }

    // ==========================================================
    // DELETE
    // ==========================================================

    public void validateDelete(ChannelPost post, String channelId, String userId) {

        if (!post.getChannelId().equals(channelId)) {
            throw new ApiException(ErrorCode.BAD_REQUEST);
        }

        if (post.isDeleted()) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }

        if (!post.getPostedByUserId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    // ==========================================================
    // POLL VALIDATION
    // ==========================================================

    private void validatePoll(CreatePollRequest poll) {

        if (poll == null) return;

        if (poll.getQuestion() == null || poll.getQuestion().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Poll question required");
        }

        if (poll.getOptions() == null ||
                poll.getOptions().size() < MIN_OPTIONS ||
                poll.getOptions().size() > MAX_OPTIONS) {

            throw new ApiException(ErrorCode.BAD_REQUEST,
                    "Options must be between 2 and 12");
        }

        if (poll.isQuizMode()) {

            if (poll.getCorrectOptionIndex() == null) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "Correct answer required");
            }

            if (poll.getCorrectOptionIndex() >= poll.getOptions().size()) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid correct answer");
            }

            if (poll.isMultipleChoice()) {
                throw new ApiException(ErrorCode.BAD_REQUEST,
                        "Quiz cannot be multiple choice");
            }
        }
    }
}