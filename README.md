# spring-boot-starter-idempotency

A Spring Boot starter that adds idempotency to any controller method with a single annotation. Requests carrying the same `Idempotency-Key` header return the cached response without re-executing the handler.

```java
@PostMapping("/payments")
@Idempotent(ttl = "24h")
public ResponseEntity<PaymentResponse> charge(@RequestBody ChargeRequest request) {
    return ResponseEntity.ok(paymentService.charge(request));
}
```

A client that retries this endpoint with the same `Idempotency-Key` will receive the original response — the handler will not run again. No code changes required beyond the annotation.

## Why this matters

Non-idempotent payment endpoints are one of the most common sources of duplicate charges in production systems. When a client times out and retries, or a mobile app loses connectivity mid-request, the server may have already processed the charge. Without idempotency, that charge runs again. With this starter, it doesn't.

## Installation

```groovy
dependencies {
    implementation 'io.github.thgrcarvalho:spring-boot-starter-idempotency:0.1.0'
}
```

> Maven Central publishing is coming. Until then, clone and `./gradlew publishToMavenLocal`.

The starter auto-configures on any `@SpringBootApplication` with Spring Web on the classpath. No `@EnableXxx` annotation needed.

## How it works

Two components wire together transparently:

1. **`IdempotencyInterceptor`** — a `HandlerInterceptor` that detects `@Idempotent` on the matched method and stamps the request with the annotation's TTL.
2. **`IdempotencyFilter`** — a `OncePerRequestFilter` that:
   - If `Idempotency-Key` is present and cached: writes the stored response and short-circuits.
   - If `Idempotency-Key` is present and not cached: wraps the response to capture the body, lets the chain execute, then stores the response.
   - If `Idempotency-Key` is absent: passes through with no overhead.

Responses with status `5xx` are never cached — server errors are assumed transient and the client should retry.

## TTL format

| Value | Duration |
|-------|----------|
| `60s` | 60 seconds |
| `30m` | 30 minutes |
| `24h` | 24 hours (default) |
| `7d`  | 7 days |

## Storage backends

Two implementations ship with the starter:

### In-memory (default)

A `ConcurrentHashMap` — zero configuration, suitable for single-instance deployments. Uses `putIfAbsent` (first writer wins) to minimise duplicate executions under concurrent retries, but does not eliminate them entirely across multiple JVMs.

### Redis (multi-instance)

A production-ready backend for horizontally-scaled deployments. Uses the Redis `SET key value NX EX seconds` primitive — a single atomic command that gives **exactly-once semantics cluster-wide**: only the first request ever writes, every concurrent retry falls back to the cached response.

Add Spring Data Redis to your app and wire up the store:

```java
@Bean
IdempotencyStore idempotencyStore(RedisConnectionFactory connectionFactory) {
    return new RedisIdempotencyStore(connectionFactory);
}
```

The autoconfiguration backs off automatically — once you declare a bean of type `IdempotencyStore`, the in-memory fallback is not instantiated.

### Custom backends

Implement `IdempotencyStore` yourself (for example, backed by a database unique constraint) and register it as a bean — same back-off behaviour applies.

## Running tests

```bash
./gradlew test
```

Integration tests spin up a real Spring Boot application and verify caching behaviour end-to-end. The Redis store tests use Testcontainers to run against a real Redis instance — Docker must be available for those to execute.

## Tech

Java 21 · Spring Boot 3 (autoconfigure, web, data-redis) · Testcontainers · Gradle · JUnit 5
