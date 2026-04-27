package com.fanzzi.backend.channel.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChannelEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(Object event) {
        publisher.publishEvent(event);
    }
}