db.user_wallets.createIndex(
{ userId: 1 },
{ unique: true, name: "user_wallet_unique_idx" }
)

db.user_wallets.createIndex(
{ updatedAt: -1 },
{ name: "wallet_updated_idx" }
)