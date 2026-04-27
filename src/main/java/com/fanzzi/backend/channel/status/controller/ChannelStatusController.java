package com.fanzzi.backend.channel.status.controller;

import com.fanzzi.backend.channel.status.dto.ChannelStatusResponse;
import com.fanzzi.backend.channel.status.dto.ChannelStatusViewerResponse;
import com.fanzzi.backend.channel.status.dto.CreateChannelStatusRequest;
import com.fanzzi.backend.channel.status.service.ChannelStatusReactionService;
import com.fanzzi.backend.channel.status.service.ChannelStatusService;
import com.fanzzi.backend.channel.status.service.ChannelStatusMediaService;
import com.fanzzi.backend.channel.status.views.ChannelStatusViewService;
import com.fanzzi.backend.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/channels/{channelId}/status")
@RequiredArgsConstructor
public class ChannelStatusController {

    private final ChannelStatusService statusService;
    private final ChannelStatusMediaService mediaService;
    private final ChannelStatusViewService viewService;
    private final ChannelStatusReactionService reactionService;

    @PostMapping("/upload-url")
    public Map<String, String> getUploadUrl(
            @PathVariable String channelId,
            @RequestParam String fileName,
            @RequestParam long fileSize
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        statusService.validateOwnerFast(channelId, userId);

        return mediaService.createUploadUrl(
                channelId,
                fileName,
                fileSize
        );
    }

    // =====================================================
    // 📤 2. CREATE STATUS
    // =====================================================
    @PostMapping
    public ChannelStatusResponse createStatus(
            @PathVariable String channelId,
            @RequestBody CreateChannelStatusRequest request
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        return statusService.create(channelId, userId, request);
    }
    // =====================================================
    // ❤️ 3. REACT
    // =====================================================
    @PostMapping("/{statusId}/react")
    public void react(
            @PathVariable String channelId,
            @PathVariable String statusId,
            @RequestParam String reaction
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        reactionService.react(statusId, channelId, userId, reaction);
    }

    @GetMapping("/{statusId}/reactions")
    public Map<String, Object> getReactions(
            @PathVariable String channelId,
            @PathVariable String statusId
    ) {
        return reactionService.getReactions(statusId);
    }

    // =====================================================
    // 📥 4. GET ACTIVE (🔥 FIXED HERE)
    // =====================================================
    @GetMapping("/active")
    public Map<String, Object> getActive(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        String userId = SecurityUtil.getCurrentUserId(); // ✅ FIX

        List<ChannelStatusResponse> list =
                statusService.getActiveStatuses(channelId, userId, page, size);

        Map<String, Object> res = new HashMap<>();
        res.put("data", list);
        res.put("count", list.size());
        res.put("hasMore", list.size() == size);
        return res;
    }

    // =====================================================
    // 🗑 5. DELETE STATUS
    // =====================================================
    @DeleteMapping("/{statusId}")
    public Map<String, Object> deleteStatus(
            @PathVariable String channelId,
            @PathVariable String statusId
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        statusService.deleteStatus(channelId, statusId, userId);

        return Map.of(
                "success", true,
                "statusId", statusId
        );
    }

    // =====================================================
    // 👁 6. MARK VIEWED
    // =====================================================
    @PostMapping("/{statusId}/view")
    public void markViewed(
            @PathVariable String channelId,
            @PathVariable String statusId
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        viewService.markViewed(statusId, userId);
    }

    // =====================================================
    // 📊 7. VIEW COUNT
    // =====================================================
    @GetMapping("/{statusId}/views/count")
    public long getViewCount(
            @PathVariable String channelId,
            @PathVariable String statusId
    ) {
        return viewService.getViewCount(statusId);
    }

    // =====================================================
    // 👁 8. CHECK UNSEEN
    // =====================================================
    @GetMapping("/{statusId}/unseen")
    public boolean isUnseen(
            @PathVariable String channelId,
            @PathVariable String statusId
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        return statusService.isUnseen(statusId, userId);
    }

    // =====================================================
    // 👥 9. VIEWERS (OWNER ONLY)
    // =====================================================
    @GetMapping("/{statusId}/views")
    public Map<String, Object> getViewers(
            @PathVariable String channelId,
            @PathVariable String statusId
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        // 🔥 OWNER VALIDATION
        statusService.validateOwnerFast(channelId, userId);

        List<ChannelStatusViewerResponse> viewers =
                viewService.getViewers(statusId);

        return Map.of(
                "data", viewers,
                "count", viewers.size()
        );
    }
}