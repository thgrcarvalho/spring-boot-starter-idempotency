package io.github.thgrcarvalho.idempotency.internal;

import io.github.thgrcarvalho.idempotency.CachedResponse;
import io.github.thgrcarvalho.idempotency.IdempotencyStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryIdempotencyStore implements IdempotencyStore {

    private record Entry(CachedResponse response, Instant expiresAt) {}

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public Optional<CachedResponse> get(String key) {
        Entry entry = store.get(key);
        if (entry == null) return Optional.empty();
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.response());
    }

    @Override
    public void put(String key, CachedResponse response, Duration ttl) {
        // putIfAbsent: first writer wins in the rare concurrent-request case
        store.putIfAbsent(key, new Entry(response, Instant.now().plus(ttl)));
    }
}
