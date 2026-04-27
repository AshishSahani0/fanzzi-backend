package com.fanzzi.backend.channel.delete;

public record RestoreResult(
        boolean success,
        long remainingMs,
        long remainingDays,
        boolean expired
) {}
