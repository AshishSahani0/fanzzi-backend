package com.fanzzi.backend.channel.subscription.service;

import com.fanzzi.backend.channel.enums.ChannelType;
import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.subscription.dto.ChannelSubscriberResponse;
import com.fanzzi.backend.channel.subscription.model.ChannelSubscription;
import com.fanzzi.backend.channel.subscription.repository.ChannelSubscriptionRepository;
import com.fanzzi.backend.common.dto.PagedResponse;
import com.fanzzi.backend.media.gateway.userprofile.UserMediaGateway;
import com.fanzzi.backend.user.model.User;
import com.fanzzi.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelSubscriberService {

    private final ChannelRepository channelRepo;
    private final ChannelSubscriptionRepository subscriptionRepo;
    private final UserRepository userRepo;
    private final UserMediaGateway userMediaGateway;

    // =====================================================
    // 👥 GET ACTIVE SUBSCRIBERS
    // =====================================================

    public PagedResponse<ChannelSubscriberResponse> getSubscribers(
            String channelId,
            int page,
            int size
    ) {

        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        if (channel.getType() == ChannelType.FREE) {
            return new PagedResponse<>(List.of(), page, size, 0, 0);
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "subscribedAt")
        );

        Page<ChannelSubscription> subs =
                subscriptionRepo.findByChannelIdAndActiveTrue(channelId, pageable);

        List<String> userIds = subs.getContent()
                .stream()
                .map(ChannelSubscription::getUserId)
                .toList();

        Map<String, User> userMap = userRepo.findByIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<ChannelSubscriberResponse> result =
                subs.getContent()
                        .stream()
                        .map(sub -> mapToResponse(sub, userMap))
                        .toList();

        return PagedResponse.from(
                new PageImpl<>(result, pageable, subs.getTotalElements())
        );
    }

    // =====================================================
    // 🔢 COUNT (O(1))
    // =====================================================

    public long getSubscriberCount(String channelId) {

        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        return channel.getType() == ChannelType.FREE
                ? 0
                : subscriptionRepo.countByChannelIdAndActiveTrue(channelId);
    }

    // =====================================================
    // 🧩 MAPPER
    // =====================================================

    private ChannelSubscriberResponse mapToResponse(
            ChannelSubscription sub,
            Map<String, User> userMap
    ) {

        User user = userMap.get(sub.getUserId());

        return ChannelSubscriberResponse.builder()
                .userId(sub.getUserId())
                .userName(user != null ? user.getUserName() : "User")
                .profileImageUrl(
                        user != null
                                ? userMediaGateway.getUserProfileUrl(user.getProfileImageKey())
                                : null
                )
                .build();
    }
}