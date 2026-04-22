package io.github.thgrcarvalho.idempotency.internal;

import io.github.thgrcarvalho.idempotency.Idempotent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Detects {@link Idempotent} on the matched handler method and stamps the request
 * with the annotation's TTL so the filter can use it after dispatch completes.
 */
public final class IdempotencyInterceptor implements HandlerInterceptor {

    static final String ATTR_ENABLED = "idempotency.enabled";
    static final String ATTR_TTL = "idempotency.ttl";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method) {
            Idempotent annotation = method.getMethodAnnotation(Idempotent.class);
            if (annotation != null) {
                request.setAttribute(ATTR_ENABLED, Boolean.TRUE);
                request.setAttribute(ATTR_TTL, annotation.ttl());
            }
        }
        return true;
    }
}
