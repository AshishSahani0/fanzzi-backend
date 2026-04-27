package com.fanzzi.backend.wallets.stars.core;

import com.fanzzi.backend.wallets.stars.dto.WalletBalanceDTO;
import com.fanzzi.backend.wallets.stars.model.UserWallet;
import com.fanzzi.backend.wallets.stars.repository.UserWalletRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WalletBalanceService {

    private final UserWalletRepository walletRepository;

    // --------------------------------------------------
    // ⭐ LIGHTWEIGHT BALANCE (CACHED)
    // --------------------------------------------------


    public WalletBalanceDTO getBalance(String userId) {

        UserWallet wallet = getOrCreateWallet(userId);

        return new WalletBalanceDTO(
                wallet.getPurchasedStars(),
                wallet.getEarnedStars()
        );
    }

    // --------------------------------------------------
    // ⭐ FULL WALLET (NO CACHE)
    // --------------------------------------------------

    public UserWallet getWallet(String userId) {
        return getOrCreateWallet(userId);
    }

    // --------------------------------------------------
    // ⭐ INTERNAL SAFE FETCH
    // --------------------------------------------------

    private UserWallet getOrCreateWallet(String userId) {

        return walletRepository.findByUserId(userId)
                .orElseGet(() ->
                        walletRepository.save(
                                UserWallet.builder()
                                        .userId(userId)
                                        .createdAt(Instant.now())
                                        .updatedAt(Instant.now())
                                        .build()
                        )
                );
    }
}