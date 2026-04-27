package com.fanzzi.backend.wallets.platform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("platform_wallet")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformWallet {

    @Id
    private String id;

    private long totalRevenue;
    private long lifetimeRevenue;

    private Instant updatedAt;
}
