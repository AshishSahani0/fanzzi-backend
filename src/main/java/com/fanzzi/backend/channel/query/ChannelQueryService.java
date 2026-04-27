package com.fanzzi.backend.channel.query;

import com.fanzzi.backend.channel.block.repository.ChannelBlockRepository;
import com.fanzzi.backend.channel.common.ChannelMapper;
import com.fanzzi.backend.channel.dto.response.ChannelResponse;
import com.fanzzi.backend.channel.enums.ChannelVisibility;
import com.fanzzi.backend.channel.membership.model.ChannelMember;
import com.fanzzi.backend.channel.membership.repository.ChannelMemberRepository;
import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.state.model.UserChannelState;
import com.fanzzi.backend.channel.state.repository.UserChannelStateRepository;
import com.fanzzi.backend.common.dto.PagedResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelQueryService {

    private final ChannelRepository channelRepository;
    private final ChannelMapper mapper;
    private final UserChannelStateRepository stateRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final ChannelBlockRepository  channelBlockRepository;

    private static final int MAX_PAGE_SIZE = 50;

    // =====================================================
    // 📦 ARCHIVED
    // =====================================================

    @Cacheable(
            value = "archived_channels",
            key = "'user:' + #userId"
    )
    public List<ChannelResponse> getArchivedChannels(String userId) {

        List<String> ids = stateRepository
                .findByUserIdAndArchivedTrue(userId)
                .stream()
                .map(UserChannelState::getChannelId)
                .toList();

        if (ids.isEmpty()) return List.of();

        return channelRepository.findAllByIdInAndDeletedFalse(ids)
                .stream()
                .map(ch -> mapper.toResponse(ch, userId))
                .toList();
    }

    // =====================================================
    // 👑 MY CHANNELS
    // =====================================================


    public PagedResponse<ChannelResponse> getMyChannels(
            String userId,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                Math.min(size, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        List<String> archivedIds =
                stateRepository.findByUserIdAndArchivedTrue(userId)
                        .stream()
                        .map(UserChannelState::getChannelId)
                        .toList();

        Page<Channel> channel =
                channelRepository.findMyChannelsExcludingArchived(
                        userId,
                        archivedIds,
                        pageable
                );

        return mapToResponse(channel, userId);
    }



    public PagedResponse<ChannelResponse> getJoinedChannels(
            String userId,
            int page,
            int size
    ) {

        // 🔥 1. Active memberships only
        List<ChannelMember> activeMembers =
                channelMemberRepository.findByUserIdAndLeftFalse(userId);

        // 🔥 2. Blocked channels
        List<String> blockedIds =
                channelBlockRepository.findByUserId(userId)
                        .stream()
                        .map(b -> b.getChannelId())
                        .toList();

        // =====================================================
        // 🔥 MERGE LOGIC
        // =====================================================
        List<String> channelIds = activeMembers.stream()
                .map(ChannelMember::getChannelId)
                .collect(java.util.stream.Collectors.toSet()) // avoid duplicates
                .stream()
                .toList();

        // add blocked channels (even if left)
        channelIds = new java.util.ArrayList<>(channelIds);
        channelIds.addAll(blockedIds);

        if (channelIds.isEmpty()) {
            return new PagedResponse<>(List.of(), page, size, 0, 0);
        }

        // =====================================================
        // 🔥 FETCH CHANNELS
        // =====================================================
        List<Channel> channels =
                channelRepository.findAllByIdInAndDeletedFalse(channelIds);

        // =====================================================
        // 🔥 FILTER ARCHIVED
        // =====================================================
        List<String> archivedIds =
                stateRepository.findByUserIdAndArchivedTrue(userId)
                        .stream()
                        .map(UserChannelState::getChannelId)
                        .toList();

        channels = channels.stream()
                .filter(ch -> !archivedIds.contains(ch.getId()))
                .toList();

        // =====================================================
        // 🔥 SORT
        // =====================================================
        channels = channels.stream()
                .sorted(java.util.Comparator.comparing(
                        Channel::getUpdatedAt,
                        java.util.Comparator.nullsLast(
                                java.util.Comparator.reverseOrder()
                        )
                ))
                .toList();

        // =====================================================
        // 🔥 PAGINATION
        // =====================================================
        int from = page * size;
        int to = Math.min(from + size, channels.size());

        if (from >= channels.size()) {
            return new PagedResponse<>(List.of(), page, size, 0, channels.size());
        }

        List<ChannelResponse> content = channels.subList(from, to)
                .stream()
                .map(ch -> mapper.toResponse(ch, userId))
                .toList();

        return new PagedResponse<>(
                content,
                page,
                size,
                (int) Math.ceil((double) channels.size() / size),
                channels.size()
        );
    }

    // =====================================================
    // 🌎 EXPLORE
    // =====================================================

    public PagedResponse<ChannelResponse> explore(
            String userId,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                Math.min(size, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "memberCount")
        );

        Page<ChannelResponse> result =
                channelRepository
                        .findByVisibilityAndDiscoverableTrueAndDeletedFalse(
                                ChannelVisibility.PUBLIC,
                                pageable
                        )
                        .map(ch -> mapper.toResponse(ch, userId));

        return PagedResponse.from(result);
    }

    // =====================================================
    // 🔧 COMMON
    // =====================================================

    private PagedResponse<ChannelResponse> mapToResponse(
            Page<Channel> page,
            String userId
    ) {
        List<ChannelResponse> content =
                page.getContent()
                        .stream()
                        .map(ch -> mapper.toResponse(ch, userId))
                        .toList();

        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }
}