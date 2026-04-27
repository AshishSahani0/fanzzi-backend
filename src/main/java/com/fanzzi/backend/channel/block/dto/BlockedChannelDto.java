package com.fanzzi.backend.channel.block.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BlockedChannelDto {

    private String id;
    private String name;
    private String avatarUrl;
}