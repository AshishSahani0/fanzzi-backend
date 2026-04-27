package com.fanzzi.backend.channel.dto.response;

import com.fanzzi.backend.channel.enums.ChannelType;
import com.fanzzi.backend.channel.enums.ChannelVisibility;
import com.fanzzi.backend.channel.model.Channel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChannelSettingsResponse {

    private String id;
    private String name;
    private String description;
    private String profileImageKey;
    private ChannelVisibility visibility;
    private ChannelType type;
    private Long monthlyPrice;

    public static ChannelSettingsResponse from(Channel c) {
        return ChannelSettingsResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .profileImageKey(c.getProfileImageKey())
                .visibility(c.getVisibility())
                .type(c.getType())
                .monthlyPrice(c.getMonthlyPrice())
                .build();
    }
}
