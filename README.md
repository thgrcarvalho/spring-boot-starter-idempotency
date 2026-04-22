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

## Custom storage backend

The default store is in-memory (`ConcurrentHashMap`) — suitable for single-instance deployments. For multi-instance environments, provide your own `IdempotencyStore` bean and the autoconfiguration backs off automatically:

```java
@Bean
IdempotencyStore redisIdempotencyStore(RedisTemplate<String, CachedResponse> redis) {
    return new RedisIdempotencyStore(redis);
}
```

## Concurrency note

The in-memory store uses `putIfAbsent` (first writer wins), which minimises duplicate executions under concurrent retries but does not eliminate them entirely. For strict exactly-once guarantees, use a custom store backed by a database unique constraint or Redis `SET NX`.

## Running tests

```bash
./gradlew test
```

Integration tests spin up a real Spring Boot application and verify caching behaviour end-to-end, including the non-`@Idempotent` pass-through case.

## Tech

Java 21 · Spring Boot 3 (autoconfigure, web) · Gradle · JUnit 5
