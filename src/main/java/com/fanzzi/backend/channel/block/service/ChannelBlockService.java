package com.fanzzi.backend.channel.block.service;

import com.fanzzi.backend.channel.block.dto.BlockedChannelDto;
import com.fanzzi.backend.channel.block.model.ChannelBlock;
import com.fanzzi.backend.channel.block.repository.ChannelBlockRepository;
import com.fanzzi.backend.channel.membership.model.ChannelMember;
import com.fanzzi.backend.channel.membership.repository.ChannelMemberRepository;
import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.media.gateway.channelprofile.ChannelMediaGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelBlockService {

    private final ChannelRepository channelRepo;
    private final ChannelMemberRepository memberRepo;
    private final ChannelBlockRepository blockRepo;
    private final ChannelMediaGateway channelMediaGateway;

    // =====================================================
    // 🚫 BLOCK CHANNEL (USER ACTION)
    // =====================================================
    @Transactional
    public void blockChannel(String channelId, String userId) {

        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.CHANNEL_NOT_FOUND,
                        "Channel not found"
                ));

        // ❌ Prevent owner blocking own channel
        if (channel.getOwnerId().equals(userId)) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "You cannot block your own channel"
            );
        }

        // ✅ Idempotent
        if (blockRepo.existsByChannelIdAndUserId(channelId, userId)) {
            return;
        }

        // =====================================================
        // 🔥 SOFT LEAVE (KEEP HISTORY)
        // =====================================================
        ChannelMember member = memberRepo
                .findByChannelIdAndUserId(channelId, userId)
                .orElse(null);

        if (member != null && !member.isLeft()) {
            member.setLeft(true);
            member.setLeftAt(Instant.now());
            memberRepo.save(member);
        }

        // =====================================================
        // 🚫 SAVE BLOCK ENTRY
        // =====================================================
        ChannelBlock block = ChannelBlock.builder()
                .channelId(channelId)
                .userId(userId)
                .blockedAt(Instant.now())
                .build();

        blockRepo.save(block);
    }

    // =====================================================
    // 🔓 UNBLOCK CHANNEL
    // =====================================================
    @Transactional
    public void unblockChannel(String channelId, String userId) {

        if (!blockRepo.existsByChannelIdAndUserId(channelId, userId)) {
            throw new ApiException(
                    ErrorCode.CHANNEL_NOT_BLOCKED,
                    "Channel is not blocked"
            );
        }

        blockRepo.deleteByChannelIdAndUserId(channelId, userId);

        // ❗ IMPORTANT:
        // Do NOT auto-join again
        // User must explicitly join
    }

    // =====================================================
    // ❓ CHECK BLOCK STATUS
    // =====================================================
    public boolean isBlocked(String channelId, String userId) {
        return blockRepo.existsByChannelIdAndUserId(channelId, userId);
    }

    // =====================================================
    // 📋 BLOCKED IDS (FAST)
    // =====================================================
    public List<String> getBlockedChannelIds(String userId) {

        return blockRepo.findByUserId(userId)
                .stream()
                .map(ChannelBlock::getChannelId)
                .toList();
    }

    // =====================================================
    // 📋 BLOCKED CHANNEL DETAILS (UI)
    // =====================================================
    public List<BlockedChannelDto> getBlockedChannels(String userId) {

        List<String> ids = getBlockedChannelIds(userId);

        if (ids.isEmpty()) return List.of();

        return channelRepo.findByIdInAndDeletedFalse(ids)
                .stream()
                .map(c -> new BlockedChannelDto(
                        c.getId(),
                        c.getName(),
                        c.getProfileImageKey() != null
                                ? channelMediaGateway.getChannelProfileUrl(c.getProfileImageKey())
                                : null
                ))
                .toList();
    }
}