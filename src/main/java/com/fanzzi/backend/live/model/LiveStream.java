package com.fanzzi.backend.live.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document("live_streams")
public class LiveStream {

    @Id
    private String id;

    private String channelId;
    private String ownerId;

    private String agoraChannel;

    private String streamKey;

    private String hlsUrl;

    private String status; // LIVE, ENDED

    private long startedAt;
    private long endedAt;



}