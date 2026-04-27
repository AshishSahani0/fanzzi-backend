package com.fanzzi.backend.media.gateway.channelpost;

import com.fanzzi.backend.media.post.PostMediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class R2PostMediaGateway implements PostMediaGateway {

    private final PostMediaStorageService postStorage;

    @Override
    public String getPostMediaDownloadUrl(String key) {
        return postStorage.getPostMediaDownloadUrl(key);
    }

    @Override
    public String getSignedUrl(String key, Duration expiry) {
        return postStorage.getSignedDownloadUrl(key, expiry);
    }

    @Override
    public void deletePostMedia(String key) {
        postStorage.deletePostMedia(key);
    }
}