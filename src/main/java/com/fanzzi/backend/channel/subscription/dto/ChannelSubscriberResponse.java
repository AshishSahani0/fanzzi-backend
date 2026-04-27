package com.fanzzi.backend.channel.subscription.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChannelSubscriberResponse {

    private String userId;
    private String userName;
    private String profileImageUrl;
}