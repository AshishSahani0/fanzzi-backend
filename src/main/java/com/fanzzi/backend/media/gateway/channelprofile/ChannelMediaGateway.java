package com.fanzzi.backend.media.gateway.channelprofile;

public interface ChannelMediaGateway {

    String getChannelProfileUrl(String key);

    void deleteChannelProfileImage(String key);



}