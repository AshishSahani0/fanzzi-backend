package com.fanzzi.backend.live.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StartLiveRequest {

    private String channelId;
    private String visibility;
    private String title;

}