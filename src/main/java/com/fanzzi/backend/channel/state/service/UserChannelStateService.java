package com.fanzzi.backend.channel.state.service;

import com.fanzzi.backend.channel.membership.repository.ChannelMemberRepository;
import com.fanzzi.backend.channel.state.model.UserChannelState;
import com.fanzzi.backend.channel.state.repository.UserChannelStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserChannelStateService {

    private final UserChannelStateRepository repo;
    private final ChannelMemberRepository memberRepository;


    @CacheEvict(
            value = {
                    "my_channels",
                    "joined_channels",
                    "archived_channels"
            },
            key = "'user:' + #userId",
            allEntries = true
    )
    public void archiveChannels(String userId, List<String> channelIds) {

        List<String> validIds = channelIds.stream()
                .filter(id -> memberRepository.existsByUserIdAndChannelIdAndLeftFalse(userId, id))
                .toList();

        if (validIds.isEmpty()) return;

        List<UserChannelState> states =
                repo.findAllByUserIdAndChannelIdIn(userId, validIds);

        Map<String, UserChannelState> map = states.stream()
                .collect(Collectors.toMap(UserChannelState::getChannelId, s -> s));

        List<UserChannelState> toSave = new ArrayList<>();

        for (String channelId : validIds) {

            UserChannelState state = map.getOrDefault(
                    channelId,
                    UserChannelState.builder()
                            .userId(userId)
                            .channelId(channelId)
                            .build()
            );

            if (!state.isArchived()) {
                state.setArchived(true);
                toSave.add(state);
            }
        }

        repo.saveAll(toSave);
    }

    @CacheEvict(
            value = {
                    "my_channels",
                    "joined_channels",
                    "archived_channels"
            },
            key = "'user:' + #userId",
            allEntries = true
    )
    public void archiveChannel(String userId, String channelId) {

        if (!memberRepository.existsByUserIdAndChannelIdAndLeftFalse(userId, channelId)) {
            throw new RuntimeException("Not allowed");
        }

        UserChannelState state =
                repo.findByUserIdAndChannelId(userId, channelId)
                        .orElseGet(() -> UserChannelState.builder()
                                .userId(userId)
                                .channelId(channelId)
                                .archived(false)
                                .build()
                        );

        if (state.isArchived()) return;

        state.setArchived(true);
        repo.save(state);
    }
    // ================= UNARCHIVE =================

    @CacheEvict(
            value = {
                    "my_channels",
                    "joined_channels",
                    "archived_channels"
            },
            allEntries = true
    )
    public void unarchiveChannel(String userId, String channelId) {

        repo.findByUserIdAndChannelId(userId, channelId)
                .ifPresent(state -> {

                    // 🔥 avoid unnecessary DB write
                    if (!state.isArchived()) return;

                    state.setArchived(false);
                    repo.save(state);
                });
    }
}