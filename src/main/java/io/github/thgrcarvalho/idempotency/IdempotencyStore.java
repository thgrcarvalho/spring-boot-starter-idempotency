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
     */
    Optional<CachedResponse> get(String key);

    /**
     * Stores a response under the given key with the specified TTL.
     */
    void put(String key, CachedResponse response, Duration ttl);
}
