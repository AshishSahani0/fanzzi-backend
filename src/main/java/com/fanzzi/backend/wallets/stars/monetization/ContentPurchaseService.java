package com.fanzzi.backend.wallets.stars.monetization;

import com.fanzzi.backend.post.model.ChannelPost;
import com.fanzzi.backend.post.repository.ChannelPostRepository;
import com.fanzzi.backend.wallets.stars.core.*;
import com.fanzzi.backend.wallets.stars.platform.PlatformRevenueService;
import com.fanzzi.backend.wallets.stars.transaction.StarTxnType;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContentPurchaseService {

    private static final double CREATOR_SHARE = 0.60;

    private final WalletMutationService mutationService;
    private final WalletTransactionService transactionService;
    private final PlatformRevenueService platformRevenueService;
    private final ChannelPostRepository postRepository;

    @Transactional
    public String purchase(
            String buyerId,
            String ignoredOwnerId,
            String channelId,
            String postId,
            long ignoredPrice
    ) {

        ChannelPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getChannelId().equals(channelId)) {
            throw new RuntimeException("Invalid channel");
        }

        String ownerId = post.getPostedByUserId();

        // ✅ OWNER → NO PAYMENT
        if (buyerId.equals(ownerId)) {
            return null;
        }

        long price = post.getPrice();

        // ✅ FREE POST
        if (price <= 0) {
            return null;
        }

        // =====================================
        // 💰 WALLET DEDUCTION
        // =====================================
        boolean deducted =
                mutationService.deductPurchasedStars(buyerId, price);

        if (!deducted) {
            throw new RuntimeException("Insufficient stars");
        }

        // =====================================
        // 💸 SPLIT REVENUE
        // =====================================
        long creatorShare = Math.round(price * CREATOR_SHARE);
        long platformShare = price - creatorShare;

        mutationService.creditEarnings(ownerId, creatorShare);
        platformRevenueService.credit(platformShare);

        // =====================================
        // 🧾 TRANSACTION ID
        // =====================================
        String txnId = generateTxnId();

        // =====================================
        // 🧾 LOG TRANSACTION
        // =====================================
        transactionService.log(
                buyerId,
                -price,
                StarTxnType.CONTENT_UNLOCK,
                channelId,
                postId,
                "Unlocked paid content | txn=" + txnId
        );

        return txnId;
    }

    // =====================================
    // 🔥 TXN GENERATOR
    // =====================================
    private String generateTxnId() {
        return "txn_" + System.currentTimeMillis() + "_" + UUID.randomUUID();
    }
}