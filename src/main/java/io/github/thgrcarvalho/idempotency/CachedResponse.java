package io.github.thgrcarvalho.idempotency;

/**
 * A snapshot of an HTTP response stored by the idempotency layer.
 */
public record CachedResponse(int status, String contentType, byte[] body) {}
