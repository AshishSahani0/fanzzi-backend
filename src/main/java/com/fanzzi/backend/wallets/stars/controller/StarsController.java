package com.fanzzi.backend.wallets.stars.controller;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.common.infrastructure.rate_limit.RateLimitService;
import com.fanzzi.backend.security.SecurityUtil;
import com.fanzzi.backend.wallets.starpack.StarPackDTO;
import com.fanzzi.backend.wallets.stars.core.WalletBalanceService;
import com.fanzzi.backend.wallets.stars.dto.BuyStarsRequest;
import com.fanzzi.backend.wallets.stars.dto.BuyStarsResponse;
import com.fanzzi.backend.wallets.stars.dto.WalletBalanceDTO;
import com.fanzzi.backend.wallets.stars.purchase.StarPurchaseService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/stars")
@RequiredArgsConstructor
public class StarsController {

    private final WalletBalanceService walletBalanceService;
    private final StarPurchaseService starPurchaseService;
    private final RateLimitService rateLimitService;
    private final  PurchaseGuardService  purchaseGuardService;

    @GetMapping("/balance")
    public WalletBalanceDTO getBalance() {
        String userId = SecurityUtil.getCurrentUserId();
        return walletBalanceService.getBalance(userId);
    }

    @PostMapping("/buy")
    public BuyStarsResponse buyStars(@RequestBody BuyStarsRequest request) {

        String userId = SecurityUtil.getCurrentUserId();

        // =====================================================
        // ⚡ RATE LIMIT (STRICT)
        // =====================================================

        rateLimitService.checkLimit(
                "rate:buy:stars:" + userId,
                5,
                Duration.ofMinutes(1)
        );

        // =====================================================
        // 🔐 VALIDATION (ZERO TRUST)
        // =====================================================

        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid amount");
        }

        if (request.getAmount() > 10000) { // cap (important)
            throw new ApiException(ErrorCode.BAD_REQUEST, "Amount too large");
        }

        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "OrderId required");
        }

        // =====================================================
        // 🧠 IDEMPOTENCY (CRITICAL FOR PAYMENTS)
        // =====================================================

        boolean alreadyProcessed = purchaseGuardService.checkAndLock(
                userId,
                request.getOrderId()
        );

        if (alreadyProcessed) {
            WalletBalanceDTO balance =
                    walletBalanceService.getBalance(userId);

            return new BuyStarsResponse(balance.getPurchasedStars());
        }

        // =====================================================
        // 💰 PURCHASE
        // =====================================================

        starPurchaseService.purchase(
                userId,
                request.getAmount(),
                request.getOrderId()
        );

        // =====================================================
        // 💳 RESPONSE
        // =====================================================

        WalletBalanceDTO balance =
                walletBalanceService.getBalance(userId);

        return new BuyStarsResponse(balance.getPurchasedStars());
    }


    @GetMapping("/packs")
    public List<StarPackDTO> getPacks() {

        return List.of(
                new StarPackDTO(50, 50, "Starter"),
                new StarPackDTO(100, 100, "Popular"),
                new StarPackDTO(200, 200, ""),
                new StarPackDTO(300, 300, ""),
                new StarPackDTO(520, 500, "Best Value"),
                new StarPackDTO(800, 750, ""),
                new StarPackDTO(1100, 1000, "Most Popular"),
                new StarPackDTO(1700, 1500, ""),
                new StarPackDTO(2050, 1800, ""),
                new StarPackDTO(2300, 2000, "Mega Pack")
        );
    }
}