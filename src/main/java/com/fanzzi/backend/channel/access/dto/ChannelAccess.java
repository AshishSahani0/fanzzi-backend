package com.fanzzi.backend.channel.access.dto;

import lombok.Builder;

@Builder
public record ChannelAccess(

        boolean member,
        boolean subscribed,

        boolean canRead,
        boolean canPost,

        boolean blurred,     // ⭐ REQUIRED
        boolean expired      // optional but useful

) {

    // ❌ No membership
    public static ChannelAccess noAccess() {
        return new ChannelAccess(
                false, false,
                false, false,
                true,  false
        );
    }

    // 🟢 FREE channel member
    public static ChannelAccess memberAccess() {
        return new ChannelAccess(
                true,  false,
                true,  true,
                false, false
        );
    }

    // 🟣 PAID active subscriber
    public static ChannelAccess subscriberAccess() {
        return new ChannelAccess(
                true,  true,
                true,  true,
                false, false
        );
    }

    // 🔒 Expired subscription
    public static ChannelAccess expiredAccess() {
        return new ChannelAccess(
                true,  false,
                false, false,
                true,  true
        );
    }
}