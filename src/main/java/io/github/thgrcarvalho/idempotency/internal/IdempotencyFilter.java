package io.github.thgrcarvalho.idempotency.internal;

import io.github.thgrcarvalho.idempotency.CachedResponse;
import io.github.thgrcarvalho.idempotency.IdempotencyStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

public final class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final IdempotencyStore store;
    private Runnable onHit;
    private Runnable onMiss;

    public IdempotencyFilter(IdempotencyStore store) {
        this(store, () -> {}, () -> {});
    }

    public IdempotencyFilter(IdempotencyStore store, Runnable onHit, Runnable onMiss) {
        this.store = store;
        this.onHit = onHit;
        this.onMiss = onMiss;
    }

    public void setOnHit(Runnable onHit) {
        this.onHit = onHit;
    }

    public void setOnMiss(Runnable onMiss) {
        this.onMiss = onMiss;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (key == null || key.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        // Cache hit: return stored response without executing the handler
        Optional<CachedResponse> cached = store.get(key);
        if (cached.isPresent()) {
            onHit.run();
            writeCached(response, cached.get());
            return;
        }

        onMiss.run();

        // Cache miss: wrap the response so we can capture the body after dispatch
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrappedResponse);

        // The interceptor will have set these attributes if the method is @Idempotent
        Boolean enabled = (Boolean) request.getAttribute(IdempotencyInterceptor.ATTR_ENABLED);
        if (Boolean.TRUE.equals(enabled) && wrappedResponse.getStatus() < 500) {
            String ttlValue = (String) request.getAttribute(IdempotencyInterceptor.ATTR_TTL);
            Duration ttl = DurationParser.parse(ttlValue != null ? ttlValue : "24h");
            store.put(key, new CachedResponse(
                    wrappedResponse.getStatus(),
                    wrappedResponse.getContentType(),
                    wrappedResponse.getContentAsByteArray()
            ), ttl);
        }

        // ContentCachingResponseWrapper buffers the body — must flush it to the real response
        wrappedResponse.copyBodyToResponse();
    }

    private void writeCached(HttpServletResponse response, CachedResponse cached) throws IOException {
        response.setStatus(cached.status());
        if (cached.contentType() != null) {
            response.setContentType(cached.contentType());
        }
        if (cached.body() != null && cached.body().length > 0) {
            response.getOutputStream().write(cached.body());
        }
    }
}
