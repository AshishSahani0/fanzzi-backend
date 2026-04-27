package com.fanzzi.backend.post.service.mapping;

import com.fanzzi.backend.media.gateway.channelpost.PostMediaGateway;
import com.fanzzi.backend.post.dto.AttachmentResponse;
import com.fanzzi.backend.post.dto.PostResponse;
import com.fanzzi.backend.post.dto.Poll;
import com.fanzzi.backend.post.dto.PollOption;
import com.fanzzi.backend.post.enums.MonetizationType;
import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.model.PollVote;
import com.fanzzi.backend.post.model.PostAttachment;
import com.fanzzi.backend.post.repository.PollVoteRepository;
import com.fanzzi.backend.post.service.poll.PollRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PostResponseMapper {

    private final PostMediaGateway mediaGateway;
    private final PollVoteRepository pollVoteRepository;
    private final PollRedisService redisService;

    // ==========================================================
    // MAIN MAPPER
    // ==========================================================

    public PostResponse map(ChannelPost post, long views, String userId) {
        return map(post, views, false, userId);
    }

    // ==========================================================
    // FULL MAPPER
    // ==========================================================

    public PostResponse map(
            ChannelPost post,
            long views,
            boolean unlocked,
            String userId
    ){

        Poll poll = mergePoll(post, userId);

        boolean isPaid = post.getMonetizationType() == MonetizationType.PAID;

        boolean canDownload =
                post.isDownloadable() &&
                        (!isPaid || unlocked);

        return PostResponse.builder()
                .id(post.getId())
                .seq(post.getSeq())
                .channelId(post.getChannelId())
                .ownerId(post.getPostedByUserId())
                .text(post.getText())

                .contentType(post.getContentType())
                .monetizationType(post.getMonetizationType())

                .attachments(mapAttachments(post.getAttachments()))

                .price(isPaid ? post.getPrice() : 0)
                .previewSeconds(isPaid ? post.getPreviewSeconds() : 0)

                .unlocked(!isPaid || unlocked)

                .edited(post.isEdited())
                .pinned(post.isPinned())
                .pinnedAt(post.getPinnedAt())

                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())

                .views(views)
                .downloadable(post.isDownloadable())
                .canDownload(canDownload)

                .poll(poll)

                .build();
    }

    // ==========================================================
    // 🔥 MERGE POLL (FIXED)
    // ==========================================================

    private Poll mergePoll(ChannelPost post, String userId) {

        Poll poll = post.getPoll(); // ✅ FIX (NO RECURSION)
        if (poll == null) return null;

        String postId = post.getId();

        // ===============================
        // REDIS STATS
        // ===============================
        Map<String, Long> stats = redisService.getStats(postId);

        long total = 0;

        for (PollOption opt : poll.getOptions()) {

            long baseVotes = opt.getVotes();
            long redisVotes = stats.getOrDefault(opt.getOptionId(), 0L);

            long finalVotes = baseVotes + redisVotes;

            if (finalVotes < 0) finalVotes = 0;

            opt.setVotes(finalVotes);
            total += finalVotes;
        }

        poll.setTotalVotes(total);

        // ===============================
        // USER VOTES
        // ===============================
        if (userId != null) {
            List<String> userVotes = pollVoteRepository
                    .findByPostIdAndUserId(postId, userId)
                    .stream()
                    .map(PollVote::getOptionId)
                    .toList();

            poll.setUserSelectedOptionIds(userVotes);
        } else {
            poll.setUserSelectedOptionIds(List.of());
        }

        return poll;
    }

    // ==========================================================
    // ATTACHMENTS
    // ==========================================================

    private List<AttachmentResponse> mapAttachments(List<PostAttachment> attachments) {

        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        return attachments.stream()
                .map(this::mapAttachment)
                .filter(a -> a != null)
                .toList();
    }

    private AttachmentResponse mapAttachment(PostAttachment attachment) {

        if (attachment == null) return null;

        return AttachmentResponse.builder()
                .key(attachment.getKey())
                .type(attachment.getType())

                .url(buildUrl(attachment.getKey()))
                .thumbnailUrl(buildUrl(attachment.getThumbnailKey()))
                .previewUrl(buildUrl(attachment.getPreviewKey()))

                .width(attachment.getWidth())
                .height(attachment.getHeight())
                .duration(attachment.getDuration())

                .fileName(attachment.getFileName())
                .fileSize(attachment.getFileSize())
                .mimeType(attachment.getMimeType())

                .build();
    }

    // ==========================================================
    // CDN URL BUILDER
    // ==========================================================

    private String buildUrl(String key) {

        if (key == null || key.isBlank()) {
            return null;
        }

        return mediaGateway.getPostMediaDownloadUrl(key);
    }
}