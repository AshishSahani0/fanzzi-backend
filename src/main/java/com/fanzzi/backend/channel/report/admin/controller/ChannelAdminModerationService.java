package com.fanzzi.backend.channel.report.admin.controller;

import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.report.moderation.enums.ChannelModerationStatus;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.report.repository.ChannelReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChannelAdminModerationService {

    private final ChannelRepository channelRepo;
    private final ChannelReportRepository reportRepo;

    // =====================================================
    // 🔧 HELPER
    // =====================================================

    private Channel getChannelOrThrow(String channelId) {
        return channelRepo.findById(channelId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );
    }

    // =====================================================
    // ⚙️ ADMIN: SET MODERATION STATUS
    // =====================================================

    public ChannelModerationStatus setStatus(
            String channelId,
            ChannelModerationStatus status
    ) {

        Channel channel = getChannelOrThrow(channelId);

        if (channel.getModerationStatus() == status) {
            return status;
        }

        channel.setModerationStatus(status);
        channelRepo.save(channel);

        return status;
    }

    // =====================================================
    // 🧹 ADMIN: CLEAR REPORT HISTORY
    // =====================================================

    public void clearReports(String channelId) {

        getChannelOrThrow(channelId);

        reportRepo.deleteByChannelId(channelId);
    }

    // =====================================================
    // 🚫 ADMIN: RESTRICT CHANNEL
    // =====================================================

    public void restrictChannel(String channelId) {
        setStatus(channelId, ChannelModerationStatus.RESTRICTED);
    }

    // =====================================================
    // ✅ ADMIN: RESTORE CHANNEL
    // =====================================================

    public void restoreChannel(String channelId) {

        Channel channel = getChannelOrThrow(channelId);

        channel.setModerationStatus(ChannelModerationStatus.NORMAL);
        channelRepo.save(channel);

        // Prevent immediate re-restriction
        reportRepo.deleteByChannelId(channelId);
    }
}

