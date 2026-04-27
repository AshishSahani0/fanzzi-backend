Here is a **production-grade README for your USER SERVICE** — designed for your **Fanzzi monolith + MongoDB + Redis architecture**, scalable to tens of millions of users.

---

# 🚀 Fanzzi — User Service (Monolith)

## 📌 Overview

The **User Service** manages user identity, profile data, phone verification, and account status.

It is designed for:

✅ High scalability (10M–50M+ users)
✅ Low latency
✅ Monolith architecture
✅ Redis acceleration
✅ Mobile-first apps
✅ Telegram/Instagram-style features

---

## 🏗 Architecture

```
Client → API Gateway (optional) → Monolith Backend
                                      │
                    ┌─────────────────┴─────────────────┐
                    │                                   │
                 MongoDB                           Redis
              (persistent data)              (cache + OTP + rate)
```

---

## 🗄 Data Storage Strategy

### 📦 MongoDB — Source of Truth

Stores long-term user data.

**Collection:** `users`

### ⚡ Redis — Fast Ephemeral Data

Used for:

* OTP storage
* Rate limiting
* Temporary verification states
* Future caching
* Presence / sessions (optional later)

---

## 👤 User Model

Collection: `users`

Key fields:

| Field                | Purpose                |
| -------------------- | ---------------------- |
| id                   | Primary identifier     |
| phone                | Unique login identity  |
| userName             | Public handle          |
| firstName / lastName | Profile info           |
| bio                  | User description       |
| email                | Optional contact       |
| profileImageKey      | Media reference        |
| active               | Account enabled flag   |
| banned               | Moderation flag        |
| deleted              | Soft delete flag       |
| lastLoginAt          | Activity tracking      |
| verified             | Official account badge |

---

## 📱 Phone Verification System

### 🔐 OTP Flow

1. User requests OTP
2. Redis stores OTP (TTL = 5 minutes)
3. User submits OTP
4. Phone updated on success

---

### ⚡ Redis Keys

```
fanzzi:otp:phone:{phone}      → OTP value
fanzzi:otp:rate:{phone}       → Rate limit lock
fanzzi:otp:attempt:{phone}    → Attempt counter
```

---

### 🚦 Protections

✅ One OTP per minute
✅ Limited verification attempts
✅ Automatic expiry
✅ No database writes until success

---

## 🔌 API Endpoints

### 👤 Profile APIs

#### Get Current User

```
GET /api/user/profile/me
```

Returns profile data of authenticated user.

---

#### Update Profile

```
PUT /api/user/profile/update
```

Request body:

```json
{
  "firstName": "Ashish",
  "lastName": "Sahani",
  "bio": "Building Fanzzi",
  "userName": "ashish",
  "email": "ashish@example.com",
  "dateOfBirth": "2001-05-20",
  "profileImageKey": "profile/123.jpg"
}
```

---

### 📲 Phone Change APIs

#### Send OTP

```
POST /api/user/profile/phone/send-otp?phone=+919999999999
```

---

#### Verify OTP & Change Phone

```
POST /api/user/profile/phone/verify
```

Params:

```
phone=+919999999999
otp=123456
```

---

## 🔐 Security

### Authentication

Requires valid JWT access token.

User ID extracted from security context.

---

### Account State Checks

Future middleware should block:

* banned users
* deleted accounts
* inactive accounts

---

## ⚡ Performance Considerations

### MongoDB

Recommended indexes:

* phone (unique)
* userName (unique, sparse)
* createdAt
* lastLoginAt

---

### Redis

Handles high-frequency operations:

* OTP
* rate limits
* temporary states

Redis operations are O(1).

---

## 🧠 Design Decisions

### Why MongoDB?

✔ Flexible schema
✔ Horizontal scalability
✔ High write throughput
✔ Ideal for user profiles

---

### Why Redis?

✔ Microsecond latency
✔ In-memory speed
✔ Atomic counters
✔ Perfect for OTP & rate limiting

---

### Why Monolith?

✔ Simpler deployment
✔ Lower latency
✔ Easier development
✔ Can scale horizontally later

---

## 🚀 Production Readiness

Designed to support:

✅ 50M+ users
✅ High concurrency
✅ Mobile workloads
✅ Real-time features (future)

---

## 🔮 Future Enhancements

Planned capabilities:

* Username availability check
* Privacy settings
* Blocking system
* Contact discovery
* Follow/subscription system
* Verification badges
* Presence (online/offline)
* Device management
* User search service
* CDN-backed media

---

## 🧪 Development Notes

OTP delivery currently mocked (console output).

Integrate with:

* Firebase SMS
* Twilio
* MSG91
* AWS SNS

---

## 🧹 Failure Handling

System protects against:

* OTP spam
* brute force attempts
* expired tokens
* invalid phone updates

---

## 🧩 Integration with Other Services

In monolith architecture, User Service can be used by:

* Auth module
* Channel module
* Subscription module
* Notification module
* Media module

---

## 📈 Scalability Path

When traffic grows:

1. Add Redis cluster
2. Enable MongoDB sharding
3. Introduce caching layers
4. Split into microservices (optional)

---

## ❤️ Maintainer Notes

User Service is the core identity layer of Fanzzi.

Changes must preserve:

* data integrity
* security
* backward compatibility
* performance

---

If you want, I can also give you:

👉 **Complete “Super App User System” blueprint (Telegram + Instagram + OnlyFans level)**
👉 Database schema for 100M users
👉 Global-scale architecture
👉 Growth-ready features

Just say:

**“Design Fanzzi user system for 100M users”** 🚀
