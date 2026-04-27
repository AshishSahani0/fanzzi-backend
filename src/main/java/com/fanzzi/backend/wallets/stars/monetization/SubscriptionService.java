package com.fanzzi.backend.wallets.stars.monetization;

import com.fanzzi.backend.channel.model.Channel;
import com.fanzzi.backend.channel.repository.ChannelRepository;
import com.fanzzi.backend.channel.subscription.model.ChannelSubscription;
import com.fanzzi.backend.channel.subscription.repository.ChannelSubscriptionRepository;
import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;

import com.fanzzi.backend.wallets.stars.core.WalletMutationService;
import com.fanzzi.backend.wallets.stars.core.WalletTransactionService;
import com.fanzzi.backend.wallets.stars.platform.PlatformRevenueService;
import com.fanzzi.backend.wallets.stars.transaction.StarTxnType;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final double CREATOR_SHARE = 0.60;
    private static final long MONTH_SECONDS = 30L * 24 * 60 * 60;

    private final WalletMutationService mutationService;
    private final WalletTransactionService transactionService;
    private final PlatformRevenueService platformRevenueService;

    private final ChannelSubscriptionRepository subscriptionRepository;
    private final ChannelRepository channelRepository;

    @Transactional
    public void subscribe(String userId, String channelId) {

        Channel channel = channelRepository.findByIdAndDeletedFalse(channelId)
                .orElseThrow(() ->
                        new ApiException(ErrorCode.NOT_FOUND, "Channel not found"));

        // 🔥 BLOCK DELETED CHANNEL
        if (channel.isDeleted()) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Channel is not available"
            );
        }

        String ownerId = channel.getOwnerId();
        long price = channel.getMonthlyPrice();

        if (userId.equals(ownerId)) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Cannot subscribe to own channel"
            );
        }

        boolean deducted = mutationService.deductPurchasedStars(userId, price);

        if (!deducted) {
            throw new ApiException(
                    ErrorCode.INVALID_REQUEST,
                    "Insufficient stars"
            );
        }

        long creatorShare = Math.round(price * CREATOR_SHARE);
        long platformShare = price - creatorShare;

        mutationService.creditEarnings(ownerId, creatorShare);
        platformRevenueService.credit(platformShare);

        transactionService.log(
                userId,
                -price,
                StarTxnType.SUBSCRIPTION,
                channelId,
                channelId,
                "Channel subscription"
        );

        Instant now = Instant.now();

        ChannelSubscription sub =
                ChannelSubscription.builder()
                        .userId(userId)
                        .channelId(channelId)
                        .channelOwnerId(ownerId)
                        .subscribedAt(now)
                        .expiresAt(now.plusSeconds(MONTH_SECONDS))
                        .active(true)
                        .pricePaid(price)
                        .build();

        subscriptionRepository.save(sub);
    }
}