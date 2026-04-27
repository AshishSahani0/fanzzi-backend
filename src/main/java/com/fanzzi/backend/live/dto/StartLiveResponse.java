package com.fanzzi.backend.live.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StartLiveResponse {

    private String liveId;
    private String agoraChannel;
    private String token;
    private String rtmpUrl;
    private String streamKey;
    private int remainingMinutes;



    public StartLiveResponse(String liveId,
                             String agoraChannel,
                             String token,
                             String rtmpUrl,
                             String streamKey,
                             int remainingMinutes) {
        this.liveId = liveId;
        this.agoraChannel = agoraChannel;
        this.token = token;
        this.rtmpUrl = rtmpUrl;
        this.streamKey = streamKey;
        this.remainingMinutes = remainingMinutes;
    }

}