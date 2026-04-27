package com.fanzzi.backend.live.agora;

public class RtcTokenBuilder {

    public enum Role {
        Role_Publisher,
        Role_Subscriber
    }

    public static String buildTokenWithUid(
            String appId,
            String appCertificate,
            String channelName,
            int uid,
            Role role,
            int expireTimestamp) throws Exception {

        AccessToken token = new AccessToken(
                appId,
                appCertificate,
                channelName,
                String.valueOf(uid)
        );

        token.addPrivilege(AccessToken.Privileges.kJoinChannel, expireTimestamp);

        if(role == Role.Role_Publisher){
            token.addPrivilege(AccessToken.Privileges.kPublishAudioStream, expireTimestamp);
            token.addPrivilege(AccessToken.Privileges.kPublishVideoStream, expireTimestamp);
            token.addPrivilege(AccessToken.Privileges.kPublishDataStream, expireTimestamp);
        }

        return token.build();
    }
}