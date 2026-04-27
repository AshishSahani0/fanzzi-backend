package com.fanzzi.backend.channel.util;

public class ChannelInviteUtil {

    public static String buildInviteLink(
            String baseUrl,
            String slug,
            String token,
            boolean isPublic
    ) {

        if (isPublic) {
            return baseUrl + "/c/" + slug;
        }

        return baseUrl + "/invite/" + token;
    }
}
