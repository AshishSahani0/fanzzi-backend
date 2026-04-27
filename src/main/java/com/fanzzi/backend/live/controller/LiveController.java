package com.fanzzi.backend.live.controller;

import com.fanzzi.backend.live.dto.StartLiveRequest;
import com.fanzzi.backend.live.dto.StartLiveResponse;
import com.fanzzi.backend.live.service.LiveService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/live")
public class LiveController {

    private final LiveService liveService;

    public LiveController(LiveService liveService) {
        this.liveService = liveService;
    }

    @PostMapping("/start")
    public StartLiveResponse startLive(
            @Valid @RequestBody StartLiveRequest request,
            Authentication authentication
    ) throws Exception {

        String ownerId = authentication.getName();

        return liveService.startLive(request, ownerId);
    }
}