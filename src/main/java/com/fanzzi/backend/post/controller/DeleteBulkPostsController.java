package com.fanzzi.backend.post.controller;

import com.fanzzi.backend.post.service.delete.BulkDeletePostsRequest;
import com.fanzzi.backend.post.service.delete.DeleteBulkPostsService;
import com.fanzzi.backend.security.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/channels/{channelId}/posts")
public class DeleteBulkPostsController {

    private final DeleteBulkPostsService service;

    // =====================================
    // 🗑️ BULK DELETE POSTS
    // =====================================
    @DeleteMapping("/bulk-delete")
    public ResponseEntity<Void> deleteBulk(
            @PathVariable String channelId,
            @Valid @RequestBody BulkDeletePostsRequest request
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        log.debug("Bulk delete request channelId={} userId={} size={}",
                channelId, userId,
                request.getPostIds() != null ? request.getPostIds().size() : 0);

        service.deleteBulk(channelId, request.getPostIds());

        log.debug("Bulk delete success channelId={} count={}",
                channelId,
                request.getPostIds() != null ? request.getPostIds().size() : 0);

        return ResponseEntity.noContent().build();
    }
}