package com.fanzzi.backend.channel.status.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatusEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(StatusEvent event) {
        publisher.publishEvent(event);
    }
}