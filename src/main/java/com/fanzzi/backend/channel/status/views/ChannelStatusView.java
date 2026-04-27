package com.fanzzi.backend.channel.status.views;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document("channel_status_views")

@CompoundIndexes({

        // 🚀 UNIQUE (fallback safety)
        @CompoundIndex(
                name = "status_user_unique",
                def = "{'statusId':1,'viewerId':1}",
                unique = true
        ),

        // 🚀 FAST OWNER VIEWERS LIST
        @CompoundIndex(
                name = "status_viewed_idx",
                def = "{'statusId':1,'viewedAt':-1}"
        ),

        // 🚀 USER ANALYTICS
        @CompoundIndex(
                name = "viewer_idx",
                def = "{'viewerId':1,'viewedAt':-1}"
        )
})
public class ChannelStatusView {

    @Id
    private String id;

    private String statusId;

    private String channelId;

    private String viewerId;

    private Instant viewedAt;

    // 🔥 NEW — device tracking (multi-device support)
    private String deviceId;

    // 🔥 NEW — for analytics (optional)
    private String sessionId;

    // 🔥 NEW — fast flag
    private boolean fromCache;


    @Indexed(expireAfter= "0s")
    private Instant expiresAt;
}