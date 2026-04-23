package io.github.thgrcarvalho.idempotency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Spring MVC controller method as idempotent.
 *
 * <p>When a request arrives with an {@code Idempotency-Key} header and a cached response
 * exists for that key, the cached response is returned immediately without executing the
 * handler. If no cached response exists, the handler executes normally and the response
 * is stored for the duration of the configured TTL.</p>
 *
 * <p>Requests without an {@code Idempotency-Key} header are always passed through.</p>
 *
 * <pre>{@code
 * @PostMapping("/payments")
 * @Idempotent(ttl = "24h")
 * public ResponseEntity<PaymentResponse> charge(@RequestBody ChargeRequest request) {
 *     return ResponseEntity.ok(paymentService.charge(request));
 * }
 * }</pre>
 *
 * <p><strong>Concurrency note:</strong> the in-memory store does not use distributed locking.
 * Two concurrent requests with the same key may both execute the handler. For strict
 * exactly-once semantics provide a custom {@link IdempotencyStore} backed by Redis or
 * a database with a unique constraint.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * How long to cache the response. Supported units: {@code d} (days), {@code h} (hours),
     * {@code m} (minutes), {@code s} (seconds). Examples: {@code "24h"}, {@code "30m"}.
     *
     * @return the TTL string, defaulting to {@code "24h"}
     */
    String ttl() default "24h";
}
