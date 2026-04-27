package com.fanzzi.backend.channel.status.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnrichedText {

    private String text;

    private List<String> links;

    private List<String> hashtags;

    private List<String> mentions;
}