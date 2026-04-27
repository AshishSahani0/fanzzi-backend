package com.fanzzi.backend.post.access;

import com.fanzzi.backend.post.postUnlock.UnlockResponse;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostAccessController {

    private final PostAccessService service;

    // =====================================================
    // UNLOCK PAID POST
    // =====================================================

    @PostMapping("/{postId}/unlock")
    public UnlockResponse unlockPost(
            @PathVariable String postId
    ) {
        String userId = SecurityUtil.getCurrentUserId();
        return service.unlockPost(postId, userId);
    }



    // =====================================================
    // RECORD VIEW
    // =====================================================

    @PostMapping("/{postId}/view")
    public void recordView(
            @PathVariable String postId
    ) {
        String userId = SecurityUtil.getCurrentUserId();
        service.recordView(postId, userId);
    }


}