User clicks JOIN
↓
API call
↓
joinChannel()

↓
[Validation]
↓
[Check member]

↓
[DB update]
→ insert / rejoin
→ memberCount++

↓
[Fetch updated channel]

↓
[Publish event] 🔥
↓
[WebSocket send] 📡
↓
Frontend receives

↓
[Return API response]