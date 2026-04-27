package com.fanzzi.backend.channel.subscription.dto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChannelSubscribersGroupedResponse {

    private List<ChannelSubscriberResponse> members;     // joined only
    private List<ChannelSubscriberResponse> subscribers; // paid
}
