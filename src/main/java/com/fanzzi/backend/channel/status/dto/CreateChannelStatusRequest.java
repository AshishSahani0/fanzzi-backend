package com.fanzzi.backend.channel.status.dto;


import lombok.Data;

import java.util.List;

@Data
public class CreateChannelStatusRequest {

    private StatusType type;

    private String text;
    private String backgroundColor; // 🎨 NEW


    private List<StatusMediaRequest> media;

    @Data
    public static class StatusMediaRequest {
        private MediaType mediaType;
        private String mediaKey;
        private Long duration;
        private Long size;
    }
}