package io.github.thgrcarvalho.idempotency.test;

import io.github.thgrcarvalho.idempotency.Idempotent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class TestController {

    public final AtomicInteger callCount = new AtomicInteger();

    @PostMapping("/idempotent")
    @Idempotent(ttl = "1h")
    public ResponseEntity<Map<String, Integer>> idempotentEndpoint() {
        return ResponseEntity.ok(Map.of("calls", callCount.incrementAndGet()));
    }

    @PostMapping("/normal")
    public ResponseEntity<Map<String, Integer>> normalEndpoint() {
        return ResponseEntity.ok(Map.of("calls", callCount.incrementAndGet()));
    }
}
