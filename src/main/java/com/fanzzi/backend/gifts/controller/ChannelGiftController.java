package com.fanzzi.backend.gifts.controller;

import com.fanzzi.backend.gifts.model.ChannelGift;
import com.fanzzi.backend.gifts.service.ChannelGiftService;
import com.fanzzi.backend.security.SecurityUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels/{channelId}/gifts")
@RequiredArgsConstructor
public class ChannelGiftController {

    private final ChannelGiftService giftService;

    @PostMapping("/send")
    public void sendGift(
            @PathVariable String channelId,
            @RequestParam String giftId
    ) {

        String userId = SecurityUtil.getCurrentUserId();

        giftService.sendGift(channelId, userId, giftId);
    }

    @GetMapping
    public List<ChannelGift> getChannelGifts(
            @PathVariable String channelId
    ) {
        return giftService.getChannelGifts(channelId);
    }
}