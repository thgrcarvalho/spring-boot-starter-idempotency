package io.github.thgrcarvalho.idempotency;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisIdempotencyStoreTest {

    private static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static RedisIdempotencyStore store;

    @BeforeAll
    static void start() {
        redis.start();

        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        store = new RedisIdempotencyStore(connectionFactory, "test-idempotency:");
    }

    @AfterAll
    static void stop() {
        if (connectionFactory != null) connectionFactory.destroy();
        redis.stop();
    }

    @Test
    void putThenGetReturnsSameResponse() {
        String key = "key-roundtrip-" + System.nanoTime();
        CachedResponse original = new CachedResponse(
                200,
                "application/json",
                "{\"result\":42}".getBytes(StandardCharsets.UTF_8)
        );

        store.put(key, original, Duration.ofSeconds(30));
        Optional<CachedResponse> fetched = store.get(key);

        assertTrue(fetched.isPresent());
        assertEquals(original.status(), fetched.get().status());
        assertEquals(original.contentType(), fetched.get().contentType());
        assertArrayEquals(original.body(), fetched.get().body());
    }

    @Test
    void getOnMissingKeyReturnsEmpty() {
        Optional<CachedResponse> result = store.get("missing-" + System.nanoTime());
        assertTrue(result.isEmpty());
    }

    @Test
    void putRespectsTtlAndKeyExpires() throws InterruptedException {
        String key = "key-ttl-" + System.nanoTime();
        CachedResponse response = new CachedResponse(204, null, new byte[0]);

        store.put(key, response, Duration.ofSeconds(1));
        assertTrue(store.get(key).isPresent());

        Thread.sleep(1500);

        assertTrue(store.get(key).isEmpty(), "key should be gone after TTL expires");
    }

    @Test
    void secondPutDoesNotOverwriteExistingEntry() {
        String key = "key-nx-" + System.nanoTime();
        CachedResponse first = new CachedResponse(200, "text/plain", "first".getBytes(StandardCharsets.UTF_8));
        CachedResponse second = new CachedResponse(500, "text/plain", "second".getBytes(StandardCharsets.UTF_8));

        store.put(key, first, Duration.ofSeconds(30));
        store.put(key, second, Duration.ofSeconds(30));

        Optional<CachedResponse> fetched = store.get(key);
        assertTrue(fetched.isPresent());
        assertEquals(200, fetched.get().status(), "first writer should win — second put must be a no-op");
        assertArrayEquals("first".getBytes(StandardCharsets.UTF_8), fetched.get().body());
    }

    @Test
    void preservesNullContentTypeAndBody() {
        String key = "key-null-" + System.nanoTime();
        CachedResponse response = new CachedResponse(304, null, null);

        store.put(key, response, Duration.ofSeconds(30));
        Optional<CachedResponse> fetched = store.get(key);

        assertTrue(fetched.isPresent());
        assertEquals(304, fetched.get().status());
        assertNull(fetched.get().contentType());
        assertNull(fetched.get().body());
    }

    @Test
    void distinguishesNullFromEmpty() {
        String keyNull = "key-null-body-" + System.nanoTime();
        String keyEmpty = "key-empty-body-" + System.nanoTime();

        store.put(keyNull, new CachedResponse(200, "text/plain", null), Duration.ofSeconds(30));
        store.put(keyEmpty, new CachedResponse(200, "text/plain", new byte[0]), Duration.ofSeconds(30));

        assertNull(store.get(keyNull).orElseThrow().body());
        assertNotNull(store.get(keyEmpty).orElseThrow().body());
        assertEquals(0, store.get(keyEmpty).orElseThrow().body().length);
    }
}
