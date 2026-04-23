package io.github.thgrcarvalho.idempotency;

import io.github.thgrcarvalho.idempotency.internal.IdempotencyFilter;
import io.github.thgrcarvalho.idempotency.internal.IdempotencyInterceptor;
import io.github.thgrcarvalho.idempotency.internal.InMemoryIdempotencyStore;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring Boot auto-configuration for the idempotency starter. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotencyStore.class)
    InMemoryIdempotencyStore inMemoryIdempotencyStore() {
        return new InMemoryIdempotencyStore();
    }

    @Bean
    IdempotencyFilter idempotencyFilter(IdempotencyStore store) {
        return new IdempotencyFilter(store);
    }

    @Configuration
    @ConditionalOnClass(MeterRegistry.class)
    static class MetricsConfiguration {

        @Autowired
        void configureMetrics(IdempotencyFilter filter, MeterRegistry registry, IdempotencyStore store) {
            Counter hits = Counter.builder("idempotency.cache.hits")
                    .description("Requests served from the idempotency cache")
                    .register(registry);
            Counter misses = Counter.builder("idempotency.cache.misses")
                    .description("Requests with an idempotency key not yet in the cache")
                    .register(registry);
            filter.setOnHit(hits::increment);
            filter.setOnMiss(misses::increment);

            if (store.size() >= 0) {
                Gauge.builder("idempotency.store.size", store, IdempotencyStore::size)
                        .description("Number of entries currently held in the idempotency store")
                        .register(registry);
            }
        }
    }

    @Bean
    FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(IdempotencyFilter filter) {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    @Bean
    IdempotencyInterceptor idempotencyInterceptor() {
        return new IdempotencyInterceptor();
    }

    @Bean
    WebMvcConfigurer idempotencyWebMvcConfigurer(IdempotencyInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}
