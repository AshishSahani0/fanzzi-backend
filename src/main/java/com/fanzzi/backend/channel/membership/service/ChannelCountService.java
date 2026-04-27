package com.fanzzi.backend.channel.membership.service;

import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChannelCountService {

    private final ChannelRepository channelRepo;

    public long getMemberCount(String channelId) {

        Channel channel = channelRepo.findMemberCountOnly(channelId);

        if (channel == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Channel not found"
            );
        }

        return channel.getMemberCount();
    }
}