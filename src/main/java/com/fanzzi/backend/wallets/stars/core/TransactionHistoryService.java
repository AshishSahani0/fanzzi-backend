package com.fanzzi.backend.wallets.stars.core;

import com.fanzzi.backend.wallets.stars.transaction.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionHistoryService {

    private final StarTransactionRepository repository;

    public Page<StarTransaction> getUserTransactions(
            String userId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return repository.findByUserId(userId, pageable);
    }
}