package com.fanzzi.backend.post.service.feed;

import com.fanzzi.backend.post.dto.PostEditedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEditedEventListener {

    @Async
    @EventListener
    public void handlePostEdited(PostEditedEvent event) {

        if (event == null || event.getChannelId() == null) {
            return;
        }

        // =====================================
        // 🚀 FUTURE EXTENSIONS HOOK
        // =====================================
        // You can safely add:
        // - analytics tracking
        // - notification triggers
        // - audit logging
        // - search index updates

        log.debug(
                "Post edited event processed channelId={} postId={} seq={}",
                event.getChannelId(),
                event.getPostId(),
                event.getSeq()
        );
    }
}