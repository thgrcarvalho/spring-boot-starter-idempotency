# Changelog

## [0.2.0] — 2026-04-23

### Added
- **`RedisIdempotencyStore`** — production-ready storage backend for multi-instance deployments.
  Uses Redis `SET key value NX EX seconds` for cluster-wide exactly-once semantics.
  Wire it up with a single `@Bean` method; the in-memory fallback backs off automatically.
- **Micrometer metrics** — when `micrometer-core` is on the classpath, the starter registers:
  - `idempotency.cache.hits` counter — requests served from cache
  - `idempotency.cache.misses` counter — requests with a key not yet cached
  - `idempotency.store.size` gauge — current entry count (in-memory store only)
- **`IdempotencyStore.size()`** default method — returns `-1` for stores that do not report size.

### Changed
- `IdempotencyFilter` no longer imports Micrometer classes directly; metrics are wired via
  `Runnable` callbacks so the filter works without Micrometer on the classpath.

## [0.1.0] — 2026-03-01

Initial release.

- `@Idempotent` annotation with configurable TTL
- `IdempotencyFilter` (request interception) + `IdempotencyInterceptor` (method detection)
- `InMemoryIdempotencyStore` with `putIfAbsent` first-writer-wins semantics
- Pluggable `IdempotencyStore` port — bring your own Redis or database backend
- Spring Boot autoconfiguration with zero `@EnableXxx` annotation required
