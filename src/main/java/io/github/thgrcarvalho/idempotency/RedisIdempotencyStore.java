package io.github.thgrcarvalho.idempotency;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.types.Expiration;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed {@link IdempotencyStore}, suitable for multi-instance deployments.
 *
 * <p>Uses Redis {@code SET key value NX EX seconds} for atomic first-writer-wins
 * semantics — concurrent retries with the same idempotency key result in the
 * handler running exactly once, cluster-wide.</p>
 *
 * <p>Wire it up as a bean to activate it; the autoconfiguration will back off:</p>
 *
 * <pre>{@code
 * @Bean
 * IdempotencyStore idempotencyStore(RedisConnectionFactory connectionFactory) {
 *     return new RedisIdempotencyStore(connectionFactory);
 * }
 * }</pre>
 */
public final class RedisIdempotencyStore implements IdempotencyStore {

    private static final String DEFAULT_KEY_PREFIX = "idempotency:";

    private final RedisConnectionFactory connectionFactory;
    private final String keyPrefix;

    /**
     * Creates a store using the default key prefix {@code "idempotency:"}.
     *
     * @param connectionFactory the Redis connection factory to use
     */
    public RedisIdempotencyStore(RedisConnectionFactory connectionFactory) {
        this(connectionFactory, DEFAULT_KEY_PREFIX);
    }

    /**
     * Creates a store with a custom key prefix.
     *
     * @param connectionFactory the Redis connection factory to use
     * @param keyPrefix         prefix prepended to every Redis key, e.g. {@code "myapp:idempotency:"}
     */
    public RedisIdempotencyStore(RedisConnectionFactory connectionFactory, String keyPrefix) {
        this.connectionFactory = connectionFactory;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public Optional<CachedResponse> get(String key) {
        try (RedisConnection conn = connectionFactory.getConnection()) {
            byte[] value = conn.stringCommands().get(redisKey(key));
            return Optional.ofNullable(value).map(RedisIdempotencyStore::deserialize);
        }
    }

    @Override
    public void put(String key, CachedResponse response, Duration ttl) {
        try (RedisConnection conn = connectionFactory.getConnection()) {
            conn.stringCommands().set(
                    redisKey(key),
                    serialize(response),
                    Expiration.from(ttl),
                    SetOption.ifAbsent()
            );
        }
    }

    private byte[] redisKey(String key) {
        return (keyPrefix + key).getBytes(StandardCharsets.UTF_8);
    }

    // Binary format: [status:int][ctLen:int][ctBytes][bodyLen:int][bodyBytes]
    // Using -1 length as a marker for null string/body so null and empty are distinguishable.
    private static byte[] serialize(CachedResponse response) {
        byte[] ct = response.contentType() == null
                ? null
                : response.contentType().getBytes(StandardCharsets.UTF_8);
        byte[] body = response.body();

        int ctLen = ct == null ? -1 : ct.length;
        int bodyLen = body == null ? -1 : body.length;
        int size = 4 + 4 + Math.max(ctLen, 0) + 4 + Math.max(bodyLen, 0);

        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.putInt(response.status());
        buf.putInt(ctLen);
        if (ct != null) buf.put(ct);
        buf.putInt(bodyLen);
        if (body != null) buf.put(body);
        return buf.array();
    }

    private static CachedResponse deserialize(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        int status = buf.getInt();
        int ctLen = buf.getInt();
        String contentType = null;
        if (ctLen >= 0) {
            byte[] ctBytes = new byte[ctLen];
            buf.get(ctBytes);
            contentType = new String(ctBytes, StandardCharsets.UTF_8);
        }
        int bodyLen = buf.getInt();
        byte[] body = null;
        if (bodyLen >= 0) {
            body = new byte[bodyLen];
            buf.get(body);
        }
        return new CachedResponse(status, contentType, body);
    }
}
