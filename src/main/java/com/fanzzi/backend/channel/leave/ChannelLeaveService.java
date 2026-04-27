package com.fanzzi.backend.channel.leave;

import com.fanzzi.backend.channel.event.ChannelEvent;
import com.fanzzi.backend.channel.event.ChannelEventPublisher;
import com.fanzzi.backend.channel.event.ChannelEventType;
import com.fanzzi.backend.channel.membership.model.ChannelMember;
import com.fanzzi.backend.channel.membership.repository.ChannelMemberRepository;
import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.subscription.repository.ChannelSubscriptionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChannelLeaveService {

    private final ChannelRepository channelRepo;
    private final ChannelMemberRepository memberRepo;
    private final ChannelEventPublisher eventPublisher;


    @Transactional
    public void leave(String channelId, String userId) {

        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );

        if (channel.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Owner cannot leave own channel"
            );
        }

        ChannelMember member = memberRepo
                .findByChannelIdAndUserId(channelId, userId)
                .orElse(null);

        if (member == null || member.isLeft()) {
            return;
        }

        member.setLeft(true);
        member.setLeftAt(Instant.now());
        memberRepo.save(member);

        long updatedCount = channelRepo.decrementMemberCountAndGet(channelId);

        eventPublisher.publish(
                new ChannelEvent(
                        channelId,
                        userId,
                        ChannelEventType.LEAVE,
                        new ChannelLeavePayload(
                                channelId,
                                userId,
                                updatedCount
                        )
                )


        );

        eventPublisher.publish(
                new ChannelCleanupEvent(channelId, userId)
        );

    }
}