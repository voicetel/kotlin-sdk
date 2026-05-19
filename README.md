# 📞 VoiceTel Kotlin SDK

The official Kotlin client for the [VoiceTel REST API](https://voicetel.com/docs/api/v2.2/) — provision numbers, place orders, validate e911, send messages, and manage your account, with `suspend` functions, coroutine-safe retries, and `kotlinx.serialization` end-to-end.

![Version](https://img.shields.io/badge/version-2.2.10-blue)
![Kotlin](https://img.shields.io/badge/kotlin-2.0%2B-purple)
![JVM](https://img.shields.io/badge/jvm-17%2B-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Coverage](https://img.shields.io/badge/coverage-88%25-brightgreen)
![Build](https://github.com/voicetel/kotlin-sdk/actions/workflows/ci.yml/badge.svg)

## 📚 Table of Contents

- [Features](#-features)
- [Installation](#-installation)
- [Quickstart](#-quickstart)
- [Authentication](#-authentication)
- [Resource Reference](#-resource-reference)
- [Error Handling](#-error-handling)
- [Cancellation](#-cancellation)
- [Rate Limits](#-rate-limits)
- [Development](#-development)
- [API Documentation](#-api-documentation)
- [Contributors](#-contributors)
- [Sponsors](#-sponsors)
- [License](#-license)

## ✨ Features

### 🛡️ Idiomatic, Strongly Typed Kotlin
- Every one of the 73 API operations is a `suspend fun` returning a `@Serializable` `data class`.
- `kotlinx.serialization.json` codec end-to-end — no Jackson, no reflection at runtime.
- Sealed `ErrorKind`/`ApiError` so you can pattern-match failures without parsing HTTP codes.

### ⚡ Coroutines-Native Transport
- Built on **Ktor client 2.3+** (CIO engine by default) — same primitives Android, Ktor servers, and Spring WebFlux apps already use.
- Cooperative cancellation: every request honors the surrounding `CoroutineScope`.
- Backoff via `kotlinx.coroutines.delay` — never blocks a thread.

### 🔁 Production-Grade Reliability
- **Automatic retry** with exponential backoff on 429 / 5xx — honors `Retry-After` headers, capped at 8s.
- **Configurable per-request timeout** via `ClientOptions`. Default 30 seconds.
- **Bearer auth** managed for you; the password→key exchange is one `client.login()` call.
- Streamlined `ApiError` carries the typed `kind`, original HTTP `statusCode`, server `code`, and structured `body` for 409 conflicts.

### 📞 Complete API Coverage
- **Numbers** — list, get, add, remove, route, translate, CNAM, LIDB, fax, forward, SMS, messaging campaigns, port-out PIN, account moves.
- **Account** — profile, sub-accounts, CDRs, credits, payments, MRC, registration, password recovery.
- **e911** — record provisioning, address validation, lookup, removal.
- **Gateways** — list, create, update, delete, view bound numbers.
- **Messaging** — SMS & MMS sending, message history, 10DLC brand and campaign registration, per-number messaging state.
- **Lookups** — CNAM and LRN dips.
- **iNumbering** — inventory search, coverage queries, number orders, port-in submissions, port-out availability checks.
- **Support** — ticket create / read / update / delete, threaded messages, replies.
- **ACL** — IP allowlist management with structured 409 conflict bodies.
- **Authentication** — switch between Digest, IP-only, or hybrid modes; rotate passwords.

### 🧪 Battle-Tested
- **Ktor `MockEngine`** test harness exercises the real transport (headers, retry, error mapping) without external mocks.
- **JaCoCo** coverage on every CI run — currently 88% line coverage.
- Builds and tests on **JDK 17 and 21** in CI.

## 🚀 Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.voicetel:voicetel-sdk-kotlin:2.2.10")
}
```

### Maven

```xml
<dependency>
  <groupId>com.voicetel</groupId>
  <artifactId>voicetel-sdk-kotlin</artifactId>
  <version>2.2.10</version>
</dependency>
```

Requires Kotlin 1.9+ and JVM 17 or later.

## 🏁 Quickstart

```kotlin
import com.voicetel.sdk.VoiceTelClient
import com.voicetel.sdk.models.Account
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = VoiceTelClient()

    // Exchange username + password for an API key (one-time per session).
    client.login(1000000001, "hunter2")

    // Typed responses — your IDE knows the shape of `me`.
    val me: Account.Data = client.account.get()
    println("Balance: $${me.cash}  |  Caller ID: ${me.callerId}")

    // List your numbers.
    client.numbers.list().numbers.forEach { n ->
        println("${n.number}  route=${n.route}  cnam=${n.cnam}  sms=${n.smsEnabled}")
    }

    client.close()
}
```

Or, if you already have an API key, configure with the DSL builder:

```kotlin
import com.voicetel.sdk.VoiceTelClient
import com.voicetel.sdk.resources.INumberingService

val client = VoiceTelClient {
    apiKey = System.getenv("VOICETEL_API_KEY")
    timeoutMillis = 30_000
    maxRetries = 2
}

val coverage = client.iNumbering.coverage(
    INumberingService.CoverageQuery(state = "NJ"))
coverage.coverage.forEach { b ->
    println("${b.npa}-${b.nxx}: ${b.count} TNs available")
}
```

## 🔑 Authentication

Every endpoint requires `Authorization: Bearer <apikey>` **except** `POST /v2.2/account/api-key`, which exchanges username + password for a fresh key. `client.login()` handles the exchange and installs the returned key on the transport.

Re-fetch the API key after any password change — the old one is invalidated.

> Don't have credentials yet? Get them at **[voicetel.com/docs/api/v2.2/credentials](https://voicetel.com/docs/api/v2.2/credentials/)**.

```kotlin
val client = VoiceTelClient()
val key: String = client.login(1000000001, "hunter2")
// `key` is the new 32-hex bearer; the client already has it installed.
```

## 🗺️ Resource Reference

| Resource | Accessor | Example |
|---|---|---|
| Account | `client.account` | `client.account.cdr(start, end)` |
| ACL | `client.acl` | `client.acl.add(Acl.ModifyRequest(entries))` |
| Authentication | `client.authentication` | `client.authentication.update(Authentication.PutRequest(authType = 1))` |
| e911 | `client.e911` | `client.e911.validate(E911.AddressRequest(...))` |
| Gateways | `client.gateways` | `client.gateways.list()` |
| iNumbering | `client.iNumbering` | `client.iNumbering.searchInventory(InventoryQuery(npa = 201))` |
| Lookups | `client.lookups` | `client.lookups.lrn("2015551234", "2012548000")` |
| Messaging | `client.messaging` | `client.messaging.send(Messaging.SendRequest(from, to, text))` |
| Numbers | `client.numbers` | `client.numbers.assignCampaign("2015551234", Numbers.CampaignAssignRequest("C1"))` |
| Support | `client.support` | `client.support.create(Support.CreateRequest("subject", "body"))` |

## 🚨 Error Handling

All HTTP errors throw `ApiError` (a `RuntimeException`) with a typed `ErrorKind`:

| `ErrorKind` | HTTP status |
|---|---|
| `BadRequest` | 400 |
| `Authentication` | 401 |
| `PermissionDenied` | 403 |
| `NotFound` | 404 |
| `Conflict` | 409 |
| `RateLimit` | 429 |
| `Server` | 5xx |
| `Unknown` | other / transport |

```kotlin
try {
    val n = client.numbers.get("9999999999")
} catch (e: ApiError) {
    when {
        e.isNotFound -> println("That number isn't on your account.")
        e.isRateLimit -> println("Slow down — backoff and retry.")
        else -> throw e
    }
}
```

For 409 conflicts on ACL or auth, the structured failure payload is on `e.body` as a `JsonElement`.

## ⏹️ Cancellation

Every call is a suspending function and cancellation-cooperative. Cancel the surrounding `Job` to abort in-flight requests:

```kotlin
val job = launch {
    val ports = client.iNumbering.ports()
    // ...
}

// Elsewhere:
job.cancel()  // any pending HTTP request and retry-backoff is cancelled.
```

`withTimeout(5_000)` also works out of the box.

## ⏱️ Rate Limits

These endpoints are limited to **6 requests per hour per IP**:

- `account/info` (`client.account.get()`)
- `account/cdr` (`client.account.cdr(...)`)
- `account/recurring-charges` (`client.account.recurringCharges()`)
- `account/payments` (`client.account.payments()`)
- `account/registration` (`client.account.registration()`)
- `account/api-key` (`client.login(...)`)

The SDK automatically retries 429 responses with `Retry-After` honored, up to `maxRetries` (default 2). To bump it:

```kotlin
VoiceTelClient {
    apiKey = System.getenv("VOICETEL_API_KEY")
    maxRetries = 4
    timeoutMillis = 60_000
}
```

## 🛠️ Development

```bash
git clone https://github.com/voicetel/kotlin-sdk
cd kotlin-sdk

# Build + test + coverage
./gradlew build

# Just tests
./gradlew test

# Open coverage report
xdg-open build/reports/jacoco/test/html/index.html
```

The build is fully self-contained via the Gradle wrapper — no system-wide Gradle install required.

### Integration tests

Live, read-only tests against the real API run when both env vars are set:

```bash
export VOICETEL_USERNAME=1000000001
export VOICETEL_PASSWORD=...
./gradlew test --tests com.voicetel.sdk.IntegrationTest
```

## 📖 API Documentation

- **Reference docs:** [voicetel.com/docs/api/v2.2/](https://voicetel.com/docs/api/v2.2/)
- **Interactive playground:** [voicetel.com/docs/api/v2.2/playground/](https://voicetel.com/docs/api/v2.2/playground/) — try the API in your browser without writing any code
- **API credentials:** [voicetel.com/docs/api/v2.2/credentials/](https://voicetel.com/docs/api/v2.2/credentials/)

## 🙌 Contributors

- [Michael Mavroudis](https://github.com/mavroudis) — Lead Developer

Contributions welcome. Open an issue describing the change, or send a pull request against `main`.

## 💖 Sponsors

| Sponsor | Contribution |
|---------|--------------|
| [VoiceTel Communications](https://voicetel.com) | Primary development and production hosting |

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
