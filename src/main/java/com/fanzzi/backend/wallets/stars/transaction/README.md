1. Fast User History
   JavaScript

db.star_transactions.createIndex(
{ userId: 1, createdAt: -1 },
{ name: "user_created_idx" }
)
🔥 2. Monthly Earnings Query
JavaScript

db.star_transactions.createIndex(
{ userId: 1, type: 1, createdAt: -1 },
{ name: "user_type_created_idx" }
)
🔥 3. Channel Earnings Aggregation
JavaScript

db.star_transactions.createIndex(
{ userId: 1, channelId: 1 },
{ name: "user_channel_idx" }
)