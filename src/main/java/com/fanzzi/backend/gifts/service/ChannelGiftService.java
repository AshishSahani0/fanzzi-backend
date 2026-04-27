package com.fanzzi.backend.gifts.service;

import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.gifts.model.ChannelGift;
import com.fanzzi.backend.gifts.model.Gift;
import com.fanzzi.backend.gifts.repository.ChannelGiftRepository;
import com.fanzzi.backend.user.model.User;
import com.fanzzi.backend.user.repository.UserRepository;
import com.fanzzi.backend.wallets.stars.core.WalletMutationService;
import com.fanzzi.backend.wallets.stars.core.WalletTransactionService;
import com.fanzzi.backend.wallets.stars.platform.PlatformRevenueService;
import com.fanzzi.backend.wallets.stars.transaction.StarTxnType;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChannelGiftService {

    private static final double CREATOR_SHARE = 0.60;

    private final WalletMutationService walletMutation;
    private final PlatformRevenueService platformRevenue;
    private final WalletTransactionService txnService;

    private final ChannelGiftRepository giftRepo;
    private final ChannelRepository channelRepo;
    private final GiftService giftService;
    private final UserRepository userRepository;

    @Transactional
    public void sendGift(
            String channelId,
            String senderId,
            String giftId
    ) {

        Channel channel = channelRepo.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        Gift gift = giftService.getGift(giftId);


        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String username = sender.getUserName(); // IMPORTANT

        String ownerId = channel.getOwnerId();

        long price = gift.getPrice();

        boolean deducted =
                walletMutation.deductPurchasedStars(senderId, price);

        if (!deducted) {
            throw new RuntimeException("Insufficient stars");
        }

        long creatorShare = Math.round(price * CREATOR_SHARE);
        long platformShare = price - creatorShare;

        walletMutation.creditEarnings(ownerId, creatorShare);

        platformRevenue.credit(platformShare);

        giftRepo.insert(
                ChannelGift.builder()
                        .channelId(channelId)
                        .senderUserId(senderId)
                        .senderUsername(username)
                        .ownerUserId(ownerId)
                        .giftId(gift.getId())
                        .giftEmoji(gift.getEmoji())
                        .giftName(gift.getName())
                        .price(price)
                        .createdAt(Instant.now())
                        .build()
        );

        txnService.log(
                senderId,
                -price,
                StarTxnType.GIFT_SENT,
                channelId,
                null,
                "Sent gift " + gift.getName()
        );
    }
    public List<ChannelGift> getChannelGifts(String channelId) {
        return giftRepo.findByChannelIdOrderByCreatedAtDesc(channelId);
    }
}