package io.github.thgrcarvalho.idempotency.internal;

import java.time.Duration;

public final class DurationParser {

    private DurationParser() {}

    public static Duration parse(String ttl) {
        if (ttl == null || ttl.isBlank()) {
            throw new IllegalArgumentException("TTL must not be blank");
        }
        String value = ttl.trim();
        try {
            if (value.endsWith("d")) return Duration.ofDays(Long.parseLong(value, 0, value.length() - 1, 10));
            if (value.endsWith("h")) return Duration.ofHours(Long.parseLong(value, 0, value.length() - 1, 10));
            if (value.endsWith("m")) return Duration.ofMinutes(Long.parseLong(value, 0, value.length() - 1, 10));
            if (value.endsWith("s")) return Duration.ofSeconds(Long.parseLong(value, 0, value.length() - 1, 10));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid TTL value: '" + ttl + "'", e);
        }
        throw new IllegalArgumentException(
                "Invalid TTL format: '" + ttl + "'. Use a number followed by d, h, m, or s (e.g. 24h, 30m)"
        );
    }
}
