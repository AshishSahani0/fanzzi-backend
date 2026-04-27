package com.fanzzi.backend.common.channel.service;

import com.fanzzi.backend.channel.membership.repository.ChannelMemberRepository;
import com.fanzzi.backend.common.channel.port.ChannelAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChannelAccessServiceImpl implements ChannelAccessService {

    private final ChannelMemberRepository memberRepo;

    @Override
    public boolean isMember(String channelId, String userId) {
        return memberRepo.existsByChannelIdAndUserId(channelId, userId);
    }
}
