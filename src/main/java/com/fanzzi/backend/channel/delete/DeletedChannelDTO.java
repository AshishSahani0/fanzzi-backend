package com.fanzzi.backend.channel.delete;




public record DeletedChannelDTO(
        String id,
        String name,
        long deletedAt,
        long remainingMs,
        long remainingDays,
        boolean expired
) {}