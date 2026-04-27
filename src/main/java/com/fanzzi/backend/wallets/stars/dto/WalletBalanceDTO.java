package com.fanzzi.backend.wallets.stars.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WalletBalanceDTO {

    private long purchasedStars;
    private long earnedStars;

}