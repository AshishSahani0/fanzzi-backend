package com.fanzzi.backend.channel.create;

import com.fanzzi.backend.channel.dto.request.CreateChannelRequest;
import com.fanzzi.backend.channel.dto.response.ChannelResponse;
import com.fanzzi.backend.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 * CHANNEL CREATION CONTROLLER
 * ============================================================
 *
 * Responsible for creating new channels for authenticated users.
 *
 * Design Principles:
 * - Owner is derived strictly from security context
 * - Request body never contains ownerId
 * - Validation handled at DTO + service layer
 * - Thin controller, business logic delegated to service
 *
 * Endpoint:
 * POST /api/channels
 */

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelCreateController {

    private final ChannelCreateService service;

    @PostMapping
    public ChannelResponse createChannel(
            @Valid @RequestBody CreateChannelRequest request
    ) {
        String ownerId = SecurityUtil.getCurrentUserId();
        return service.createChannel(ownerId, request);
    }
}