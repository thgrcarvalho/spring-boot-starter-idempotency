package io.github.thgrcarvalho.idempotency;

import io.github.thgrcarvalho.idempotency.internal.IdempotencyFilter;
import io.github.thgrcarvalho.idempotency.internal.IdempotencyInterceptor;
import io.github.thgrcarvalho.idempotency.internal.InMemoryIdempotencyStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
