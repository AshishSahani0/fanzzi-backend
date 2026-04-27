package com.fanzzi.backend.wallets.stars.core;

import com.fanzzi.backend.wallets.stars.transaction.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WalletTransactionService {

    private final StarTransactionRepository transactionRepository;

    public void log(
            String userId,
            long amount,
            StarTxnType type,
            String channelId,
            String referenceId,
            String description
    ) {

        transactionRepository.save(
                StarTransaction.builder()
                        .userId(userId)
                        .amount(amount)
                        .type(type)
                        .channelId(channelId)
                        .referenceId(referenceId)
                        .description(description)
                        .createdAt(Instant.now())
                        .build()
        );
    }
}