package com.fanzzi.backend.wallets.stars.purchase;

import com.fanzzi.backend.common.exception.ApiException;
import com.fanzzi.backend.common.exception.ErrorCode;
import com.fanzzi.backend.wallets.stars.core.WalletMutationService;
import com.fanzzi.backend.wallets.stars.core.WalletTransactionService;
import com.fanzzi.backend.wallets.stars.transaction.StarTxnType;
import com.fanzzi.backend.wallets.stars.transaction.StarTransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StarPurchaseService {

    private final WalletMutationService mutationService;
    private final WalletTransactionService transactionService;
    private final StarTransactionRepository transactionRepository;

    // TEMP MODE – Replace with webhook validation later
    public void purchase(String userId, long amount, String orderId) {

        if (amount <= 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid purchase amount");
        }

        if (orderId == null || orderId.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Order ID required");
        }

        // 🔐 Idempotency check (very important)
        boolean alreadyProcessed =
                transactionRepository.existsByUserIdAndReferenceId(userId, orderId);

        if (alreadyProcessed) {
            return; // Prevent duplicate purchase
        }

        // 1️⃣ Credit stars
        mutationService.addPurchasedStars(userId, amount);

        // 2️⃣ Log transaction
        transactionService.log(
                userId,
                amount,
                StarTxnType.PURCHASE,
                null,
                orderId,
                "Star purchase (temporary mode)"
        );
    }
}