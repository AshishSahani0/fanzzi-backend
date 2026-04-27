## 📘 FANZZI Auth Module — README (Monolith • Production-Ready)

Authentication system for a high-scale app built with **Spring Boot + MongoDB + Redis + JWT** in a **single monolith** (no microservices required).

---

# 🧠 Overview

This module handles:

* 📱 Phone authentication (Firebase OTP)
* 👤 User & 👑 Admin login
* 🎟 JWT access tokens (stateless)
* 🔄 Device-scoped refresh tokens (Redis)
* 🧠 Redis session management
* 📲 Device registration & tracking
* 🚫 Ban / deactivate enforcement
* 🚦 OTP rate limiting (phone + IP)
* 🏗 Monolith-friendly design
* 🔐 Production security patterns

---

# 🏗 Architecture

## 🔥 Stateless + Stateful Hybrid (Industry Standard)

| Component     | Storage | Purpose                    |
| ------------- | ------- | -------------------------- |
| Access Token  | Client  | Fast API authentication    |
| Refresh Token | Redis   | Secure re-login            |
| Session       | Redis   | Account state enforcement  |
| Users         | MongoDB | Persistent identity        |
| Devices       | MongoDB | Device registry & security |
| OTP Limits    | Redis   | Abuse protection           |

---

# 👤 User Authentication Flow

## 🔐 Login (Firebase Phone)

### Endpoint

```
POST /auth/user/login
```

### Headers

```
Authorization: Bearer <firebase-id-token>
X-Device-Id: <unique-device-id>
X-Country-Code: <country-code>
```

### Flow

1. Verify Firebase ID token
2. Extract phone number
3. Find or create AuthUser (MongoDB)
4. Validate account status
5. Register/update device
6. Create Redis session (device-scoped)
7. Generate JWT access token
8. Generate refresh token
9. Set refresh cookie

---

## 🔄 Refresh Access Token

```
POST /auth/user/refresh
```

### Requirements

* Cookie: `userRefreshToken`
* Header: `X-Device-Id`

### Security

* ✅ Token rotation
* ✅ Device binding
* ✅ Hash stored in Redis (never raw)
* ✅ Replay protection

---

## 🚪 Logout (Current Device)

```
POST /auth/user/logout
```

Actions:

* Revoke refresh token (Redis)
* Clear device session
* Remove cookie

---

# 👑 Admin Authentication

## 🔐 Admin Login

```
POST /auth/admin/login
```

### Required

* Email
* Password
* Master code (2FA-style)
* Device ID

### Security Layers

* ✅ Credential validation
* ✅ Second factor (master code)
* ✅ Device-scoped session
* ✅ Refresh token issuance

---

## 🔄 Admin Refresh

Same mechanism as users using:

```
adminRefreshToken cookie
```

---

## 🚪 Admin Logout

Removes:

* Refresh token
* Session
* Cookie

---

# 🎟 JWT System

## Access Token (Short-Lived)

Stateless — used on every API request.

### Claims

```
sub   → userId
role  → USER / ADMIN
type  → access
iss   → fanzzi-auth
jti   → unique token id
exp   → expiration
```

---

## Refresh Token (Long-Lived)

Stored hashed in Redis.

```
type → refresh
sub  → userId
```

---

# 🧠 Redis Session System

Redis is the **source of truth for active sessions**.

### Enforces

* Account ban
* Deactivation
* Device logout
* Session expiration

### Key Format

```
session:user:{userId}:{deviceId}
```

Device index:

```
session:user:{userId}:devices
```

---

# 🔄 Refresh Token Storage

### Key Format

```
refresh:{userId}:{deviceId}
```

Stored value:

```
SHA-256 hash of refresh token
```

Device index:

```
refresh:{userId}:devices
```

---

# 📲 Device Registry (MongoDB)

Collection: `user_devices`

Tracks trusted devices, push tokens, and activity.

### Key Fields

* userId
* deviceId (unique per user)
* platform / OS / app version
* IP address & user agent
* FCM token
* lastActiveAt

---

# 🗄 Auth User Model (MongoDB)

Collection: `auth_users`

```json
{
  "id": "ObjectId",
  "phone": "+91xxxxxxxxxx",
  "countryCode": "IN",
  "role": "USER",
  "active": true,
  "banned": false,
  "createdAt": "...",
  "lastLoginAt": "..."
}
```

---

# 🚦 OTP Rate Limiting

Redis-based protection against abuse.

### Keys

```
fanzzi:otp:phone:{phone}
fanzzi:otp:ip:{ip}
```

### Default Limits

| Type  | Limit       | Window |
| ----- | ----------- | ------ |
| Phone | 3 requests  | 1 min  |
| IP    | 10 requests | 1 min  |

Dev profile disables limits.

---

# 🔒 Security Features

## Account Protection

* ✅ Ban enforcement via session
* ✅ Device-scoped authentication
* ✅ Refresh token rotation
* ✅ Replay attack protection
* ✅ Hashed token storage
* ✅ Stateless access tokens
* ✅ Secure cookies
* ✅ Clock-skew tolerance

---

## Device Security

* Trusted device tracking
* Multi-device sessions
* Device-specific logout
* Optional device blocking

---

## API Protection (Spring Security)

* Stateless mode
* JWT filter
* Session validation via Redis
* Custom JSON error responses
* No form login / HTTP Basic

---

# ⚙️ Configuration

## JWT

```
jwt.secret=<min 32 chars>
jwt.access-expiration=900000
jwt.refresh-expiration=2592000000
```

---

## MongoDB

```
spring.data.mongodb.uri=mongodb://localhost:27017/fanzzi_auth
```

---

## Redis

```
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

---

## Admin Credentials

```
admin.email=admin@fanzzi.com
admin.password=<secure-password>
admin.master-code=<secure-code>
```

---

# 📂 Suggested Folder Structure

```
auth
├── adminauth
├── userauth
├── device
├── jwt
├── refresh
├── session
├── otp
├── model
├── repository
├── config
├── security
└── common
```

---

# 🚀 Scaling Characteristics

Designed for:

* Millions of concurrent users
* Horizontal scaling
* Stateless API nodes
* Shared Redis + MongoDB
* CDN-friendly architecture

Multiple instances can run behind a load balancer because:

* Sessions stored in Redis
* Tokens stateless
* MongoDB shared database

---

# 🧪 Development Mode

```
spring.profiles.active=dev
```

Changes:

* Firebase verification bypassed
* OTP limits disabled
* Sessions read from DB instead of Redis
* Easier local testing

---

# ❤️ Health Check

```
GET /health
```

Used by load balancers or monitoring tools.

---

# 🏆 Why This Design Is Industry-Level

This pattern mirrors systems used by:

* Telegram
* Discord
* Instagram
* Uber
* Netflix
* Banking apps

Core principle:

> 🔥 Stateless authentication + stateful control

---

If you want next, I can give you:

💎 FANZZI **full backend architecture (10M–50M users)**
💎 Redis cluster + Mongo sharding plan
💎 Real-time chat architecture
💎 Media storage (R2 + CDN)
💎 Kubernetes deployment blueprint
💎 Zero-downtime scaling guide

Just say 👉 **“Full FANZZI architecture”**
