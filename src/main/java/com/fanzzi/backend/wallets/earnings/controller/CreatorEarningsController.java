package com.fanzzi.backend.wallets.earnings.controller;

import com.fanzzi.backend.security.SecurityUtil;
import com.fanzzi.backend.wallets.earnings.dto.ChannelEarningsResponse;
import com.fanzzi.backend.wallets.earnings.dto.EarningsSummaryResponse;
import com.fanzzi.backend.wallets.stars.core.WalletBalanceService;
import com.fanzzi.backend.wallets.stars.transaction.*;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/creator/earnings")
@RequiredArgsConstructor
public class CreatorEarningsController {

    private final WalletBalanceService walletBalanceService;
    private final StarTransactionRepository txnRepo;

    // --------------------------------------------------
    // ⭐ Earnings Summary
    // --------------------------------------------------

    @GetMapping("/summary")
    public EarningsSummaryResponse getSummary() {

        String userId = SecurityUtil.getCurrentUserId();

        var wallet = walletBalanceService.getWallet(userId);

        long totalEarned = wallet.getLifetimeEarned();
        long available = wallet.getEarnedStars();

        // Monthly earnings (DB filtered)
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);

        long monthly = txnRepo
                .findByUserIdAndTypeAndCreatedAtAfter(
                        userId,
                        StarTxnType.GIFT_RECEIVED,
                        since
                )
                .stream()
                .mapToLong(StarTransaction::getAmount)
                .sum();

        return EarningsSummaryResponse.builder()
                .totalEarned(totalEarned)
                .monthlyEarned(monthly)
                .available(available)
                .build();
    }

    // --------------------------------------------------
    // ⭐ Earnings by Channel
    // --------------------------------------------------

    @GetMapping("/channels")
    public ChannelEarningsResponse getChannelEarnings() {

        String userId = SecurityUtil.getCurrentUserId();

        List<ChannelEarningProjection> results =
                txnRepo.aggregateChannelEarnings(userId);

        Map<String, Long> earningsByChannel =
                results.stream()
                        .filter(r -> r.getId() != null)
                        .collect(Collectors.toMap(
                                ChannelEarningProjection::getId,
                                ChannelEarningProjection::getTotal
                        ));

        return ChannelEarningsResponse.builder()
                .channels(earningsByChannel)
                .build();
    }
}