package io.github.thgrcarvalho.idempotency;

import java.time.Duration;
import java.util.Optional;

/**
 * Storage backend for idempotency keys and their cached responses.
 *
 * <p>The default implementation is {@code InMemoryIdempotencyStore}, which is suitable
 * for single-instance deployments. For multi-instance or distributed environments,
 * provide a custom implementation backed by Redis or a database and register it as
 * a Spring bean — the autoconfiguration will back off automatically.</p>
 *
 * <pre>{@code
 * @Bean
 * IdempotencyStore idempotencyStore(RedisConnectionFactory connectionFactory) {
 *     return new RedisIdempotencyStore(connectionFactory);
 * }
 * }</pre>
 */
public interface IdempotencyStore {

    /**
     * Returns the cached response for the given key, or empty if not found or expired.
     *
     * @param key the idempotency key from the {@code Idempotency-Key} request header
     * @return the cached response, or {@link java.util.Optional#empty()} if absent or expired
     */
    Optional<CachedResponse> get(String key);

    /**
     * Stores a response under the given key with the specified TTL.
     *
     * @param key      the idempotency key
     * @param response the response to cache
     * @param ttl      how long to retain the entry
     */
    void put(String key, CachedResponse response, Duration ttl);

    /**
     * Returns the number of entries currently held in this store, or {@code -1} if the
     * implementation does not support reporting its size (e.g. Redis-backed stores).
     *
     * @return the current entry count, or {@code -1} if unsupported
     */
    default long size() {
        return -1L;
    }
}
