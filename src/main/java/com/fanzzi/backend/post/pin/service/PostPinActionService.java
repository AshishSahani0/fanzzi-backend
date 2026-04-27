package com.fanzzi.backend.post.pin.service;



public interface PostPinActionService {

    boolean pinPost(String channelId, String postId);

    boolean unpinPost(String channelId, String postId);
}