package io.github.thgrcarvalho.idempotency;

import io.github.thgrcarvalho.idempotency.internal.IdempotencyFilter;
import io.github.thgrcarvalho.idempotency.test.TestApplication;
import io.github.thgrcarvalho.idempotency.test.TestController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IdempotencyIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestController controller;

    @Autowired
    IdempotencyStore store;

    @BeforeEach
    void resetCounter() {
        controller.callCount.set(0);
        // Replace the store contents by re-injecting a fresh one would be complex;
        // instead we use unique keys per test via the test method name convention
    }

    @Test
    void sameKeyReturnsCachedResponseWithoutReExecuting() {
        String key = "test-same-key-" + System.nanoTime();

        ResponseEntity<Map> first = post("/idempotent", key);
        ResponseEntity<Map> second = post("/idempotent", key);

        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals(HttpStatus.OK, second.getStatusCode());
        assertEquals(first.getBody(), second.getBody());
        assertEquals(1, controller.callCount.get(), "handler should only execute once");
    }

    @Test
    void differentKeysExecuteHandlerEachTime() {
        String key1 = "test-key1-" + System.nanoTime();
        String key2 = "test-key2-" + System.nanoTime();

        ResponseEntity<Map> first = post("/idempotent", key1);
        ResponseEntity<Map> second = post("/idempotent", key2);

        assertNotEquals(first.getBody(), second.getBody());
        assertEquals(2, controller.callCount.get());
    }

    @Test
    void requestWithoutKeyAlwaysExecutes() {
        post("/idempotent", null);
        post("/idempotent", null);

        assertEquals(2, controller.callCount.get(), "requests without key must not be cached");
    }

    @Test
    void nonAnnotatedEndpointIsNotCachedEvenWithKey() {
        String key = "test-normal-" + System.nanoTime();

        ResponseEntity<Map> first = post("/normal", key);
        ResponseEntity<Map> second = post("/normal", key);

        assertNotEquals(first.getBody(), second.getBody(), "non-@Idempotent endpoint must not be cached");
        assertEquals(2, controller.callCount.get());
    }

    @Test
    void cachedResponsePreservesStatusCode() {
        String key = "test-status-" + System.nanoTime();

        ResponseEntity<Map> first = post("/idempotent", key);
        ResponseEntity<Map> second = post("/idempotent", key);

        assertEquals(first.getStatusCode(), second.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> post(String path, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        if (idempotencyKey != null) {
            headers.set(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        }
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    }
}
