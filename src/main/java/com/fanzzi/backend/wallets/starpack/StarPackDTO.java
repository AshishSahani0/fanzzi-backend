package com.fanzzi.backend.wallets.starpack;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StarPackDTO {

    private int stars;
    private double price;
    private String badge;
}