package com.fanzzi.backend.wallets.stars.dto;

import lombok.Data;

@Data
public class BuyStarsRequest {

    private Long amount;

    // Temporary mode → client sends random orderId
    // Later this will come from Razorpay/Stripe webhook
    private String orderId;
}