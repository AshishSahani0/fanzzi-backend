package com.fanzzi.backend.post.pin.service;

public record PostPinEvent(
        String channelId,
        String postId,
        boolean pinned
) {}