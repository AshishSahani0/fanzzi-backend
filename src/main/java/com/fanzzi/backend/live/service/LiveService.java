package com.fanzzi.backend.live.service;

import com.fanzzi.backend.live.dto.StartLiveRequest;
import com.fanzzi.backend.live.dto.StartLiveResponse;
import com.fanzzi.backend.live.model.LiveStream;
import com.fanzzi.backend.live.premium.model.CreatorLivePlan;
import com.fanzzi.backend.live.premium.repository.CreatorLivePlanRepository;
import com.fanzzi.backend.live.repository.LiveRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LiveService {

    private final LiveRepository liveRepository;
    private final CreatorLivePlanRepository planRepository;
    private final AgoraTokenService agoraTokenService;

    public LiveService(LiveRepository liveRepository,
                       CreatorLivePlanRepository planRepository,
                       AgoraTokenService agoraTokenService) {
        this.liveRepository = liveRepository;
        this.planRepository = planRepository;
        this.agoraTokenService = agoraTokenService;
    }

    public StartLiveResponse startLive(StartLiveRequest req, String ownerId) throws Exception {

        CreatorLivePlan plan =
                planRepository.findByOwnerIdAndActive(ownerId, true)
                        .orElseThrow(() -> new RuntimeException("Premium required"));

        int remaining = plan.getTotalMinutes() - plan.getUsedMinutes();

        if (remaining <= 0) {
            throw new RuntimeException("No live minutes left");
        }

        String liveId = UUID.randomUUID().toString();

        String agoraChannel = "live_" + liveId;

        String streamKey = UUID.randomUUID().toString();

        String token = agoraTokenService.generateHostToken(agoraChannel);

        LiveStream live = new LiveStream();

        live.setId(liveId);
        live.setChannelId(req.getChannelId());
        live.setOwnerId(ownerId);
        live.setAgoraChannel(agoraChannel);
        live.setStreamKey(streamKey);

        // ✅ ADD THIS PART HERE
        String hlsUrl = "https://cdn.fanzzi.com/live/" + streamKey + ".m3u8";
        live.setHlsUrl(hlsUrl);

        live.setStatus("LIVE");
        live.setStartedAt(System.currentTimeMillis());

        liveRepository.save(live);

        return new StartLiveResponse(
                liveId,
                agoraChannel,
                token,
                "rtmp://push.agora.io/live",
                streamKey,
                remaining
        );
    }
}