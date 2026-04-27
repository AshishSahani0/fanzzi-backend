package com.fanzzi.backend.media.gateway.channelpost;

import java.time.Duration;

public interface PostMediaGateway {

    String getPostMediaDownloadUrl(String key);
    String getSignedUrl(String key, Duration expiry);

    void deletePostMedia(String key);
}