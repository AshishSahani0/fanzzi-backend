package com.fanzzi.backend.auth.events;

public record UserCreatedEvent(
        String userId,
        String phone,
        String countryCode
) {}