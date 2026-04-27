package com.fanzzi.backend.post.pin.controller;

import com.fanzzi.backend.post.pin.service.PostPinActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/channels/{channelId}/posts")
public class PostPinActionController {

    private final PostPinActionService postPinActionService;

    @PostMapping("/{postId}/pin")
    public void pinPost(
            @PathVariable String channelId,
            @PathVariable String postId
    ) {
        postPinActionService.pinPost(channelId, postId);
    }

    @DeleteMapping("/{postId}/pin")
    public void unpinPost(
            @PathVariable String channelId,
            @PathVariable String postId
    ) {
        postPinActionService.unpinPost(channelId, postId);
    }
}