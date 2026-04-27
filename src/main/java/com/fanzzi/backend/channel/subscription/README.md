// Prevent duplicate subscription
db.channel_subscriptions.createIndex(
{ userId: 1, channelId: 1 },
{ unique: true, name: "unique_user_channel_subscription" }
)

// Fast pagination
db.channel_subscriptions.createIndex(
{ channelId: 1, subscribedAt: -1 },
{ name: "channel_subscribed_idx" }
)

// Active subscription lookup
db.channel_subscriptions.createIndex(
{ channelId: 1, active: 1 },
{ name: "channel_active_idx" }
)

db.channel_subscriptions.createIndex(
{ userId: 1, channelId: 1, active: 1, expiresAt: 1 },
{ name: "subscription_access_idx" }
)

