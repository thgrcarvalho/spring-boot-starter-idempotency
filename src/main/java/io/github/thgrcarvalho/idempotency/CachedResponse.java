package io.github.thgrcarvalho.idempotency;

/**
 * A snapshot of an HTTP response stored by the idempotency layer.
 *
 * @param status      the HTTP status code
 * @param contentType the value of the {@code Content-Type} header, or {@code null}
 * @param body        the raw response body bytes, or {@code null}
 */
public record CachedResponse(int status, String contentType, byte[] body) {}
