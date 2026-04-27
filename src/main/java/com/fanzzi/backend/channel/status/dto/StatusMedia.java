package com.fanzzi.backend.channel.status.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatusMedia {

    private MediaType mediaType; // IMAGE, VIDEO, AUDIO
    private String mediaKey;
    private Long duration;

}
