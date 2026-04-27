# 📱 Device Management Module (User Device Tracking)

## 📦 Package

`com.fanzzi.backend.auth.device`

---

# 🚀 Overview

This module manages **user devices, sessions, and security metadata** in the system.

It enables:

* Multi-device login support
* Device-level session control
* Suspicious activity detection
* Future features like *“Logout from other devices”*

---

# 🧠 Architecture

```
device/
 ├── model/
 │     └── UserDevice.java
 ├── repository/
 │     └── UserDeviceRepository.java
 ├── service/
       └── UserDeviceService.java
```

---

# 📘 1. UserDevice Model

## 📂 `UserDevice.java`

Represents a **single device used by a user**

---

## 🔑 Core Identity

| Field      | Description                            |
| ---------- | -------------------------------------- |
| `id`       | MongoDB primary key                    |
| `userId`   | Owner of device                        |
| `deviceId` | Unique device identifier (from client) |

👉 Unique constraint:

```java
@CompoundIndex(userId + deviceId)
```

✔ Prevents duplicate devices
✔ Ensures 1 device = 1 session

---

## 🔔 Push Notifications

| Field      | Description                    |
| ---------- | ------------------------------ |
| `fcmToken` | Firebase Cloud Messaging token |

👉 Used for:

* Push notifications
* Real-time updates

---

## 📱 Device Information

| Field        | Example             |
| ------------ | ------------------- |
| `platform`   | ANDROID / IOS / WEB |
| `deviceName` | Pixel 7 / iPhone 15 |
| `osVersion`  | Android 14          |
| `appVersion` | 1.0.3               |

👉 Sent from frontend (Flutter / Web)

---

## 🌐 Security Information

| Field         | Purpose                |
| ------------- | ---------------------- |
| `ipAddress`   | Last known IP          |
| `userAgent`   | Browser / app info     |
| `fingerprint` | SHA256(ip + userAgent) |

👉 Used for:

* Session validation
* Device binding
* Anti-hijacking protection

---

## 🛡 Security Flags

| Field        | Meaning                   |
| ------------ | ------------------------- |
| `trusted`    | Device is trusted         |
| `blocked`    | Device is blocked         |
| `suspicious` | Unusual activity detected |

---

## ⚠️ Suspicious Detection

| Field            | Description          |
| ---------------- | -------------------- |
| `lastIpChangeAt` | When IP last changed |

👉 Triggered when:

* Same device logs in from different IP

---

## ⏱ Activity Tracking

| Field          | Purpose                  |
| -------------- | ------------------------ |
| `lastActiveAt` | Last request timestamp   |
| `createdAt`    | Device registration time |

---

# 🧩 2. Repository Layer

## 📂 `UserDeviceRepository.java`

Handles database operations.

### 🔍 Methods

```java
findByUserIdAndDeviceId(userId, deviceId)
```

👉 Get specific device

```java
findByUserId(userId)
```

👉 Get all devices of user

```java
deleteByUserIdAndDeviceId(userId, deviceId)
```

👉 Remove device (logout)

---

# ⚙️ 3. Service Layer

## 📂 `UserDeviceService.java`

Core business logic for device management.

---

## 🔥 registerDevice()

### Purpose:

* Create OR update device
* Track activity
* Detect suspicious behavior

---

### 🔁 Flow:

#### 1. Find existing device

```java
repo.findByUserIdAndDeviceId(...)
```

#### 2. Generate fingerprint

```java
SHA256(ip + userAgent)
```

---

### 🚨 3. Detect suspicious login

```java
if (IP changed)
→ mark suspicious = true
→ set lastIpChangeAt
```

---

### 🧹 4. Safe updates

* Avoid null overwrite
* Trim values
* Apply defaults

---

### ⏱ 5. Update activity

```java
lastActiveAt = now
```

---

### 💾 6. Save to DB

```java
repo.save(device)
```

---

## 🔍 getUserDevices()

Returns all devices of a user

👉 Used for:

* Device management UI
* Security dashboards

---

## 🚫 removeDevice()

Deletes device

👉 Used for:

* Logout
* Remove compromised device

---

# 🔐 Security Design

This module provides:

### ✅ Device Binding

* Token tied to deviceId

### ✅ Fingerprint Validation

* Based on IP + User-Agent

### ✅ Suspicious Detection

* Detect IP changes

### ✅ Session Control

* Per-device session management

---

# ⚡ Performance

### Indexes Used:

```json
{ "userId": 1, "deviceId": 1 }  // UNIQUE
```

👉 Fast lookup for:

* Login
* Session validation

---

# 🚀 Future Extensions

You can build:

### 🔥 Device Management UI

* Show active devices
* Show last active time

### 🔥 Security Alerts

* “New login detected”

### 🔥 Device Actions

* Logout other devices
* Trust / Block device

---

# 🧠 Summary

This module converts your auth system from:

❌ Basic login system
➡️
✅ Production-grade **multi-device secure authentication system**

---

# 💡 Key Highlights

* Device-level sessions
* Fingerprint-based security
* Redis + Mongo hybrid system
* Fully scalable architecture

---

# 🔥 Status

✅ Production-ready
✅ Secure
✅ Extensible

---
