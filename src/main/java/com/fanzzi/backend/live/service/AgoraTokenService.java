package com.fanzzi.backend.live.service;

import com.fanzzi.backend.live.agora.RtcTokenBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AgoraTokenService {

    @Value("${agora.appId}")
    private String appId;

    @Value("${agora.appCertificate}")
    private String appCertificate;

    public String generateHostToken(String channelName) throws Exception {

        int uid = 0;

        int expireTime = (int) (System.currentTimeMillis() / 1000) + 3600;

        return RtcTokenBuilder.buildTokenWithUid(
                appId,
                appCertificate,
                channelName,
                uid,
                RtcTokenBuilder.Role.Role_Publisher,
                expireTime
        );
    }
}