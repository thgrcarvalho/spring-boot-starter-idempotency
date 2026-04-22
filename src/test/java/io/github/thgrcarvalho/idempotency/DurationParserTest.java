package io.github.thgrcarvalho.idempotency;

import io.github.thgrcarvalho.idempotency.internal.DurationParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationParserTest {

    @ParameterizedTest
    @CsvSource({
            "1s,  1",
            "60s, 60",
            "1m,  60",
            "30m, 1800",
            "1h,  3600",
            "24h, 86400",
            "1d,  86400",
            "7d,  604800"
    })
    void parsesValidDurationsToSeconds(String input, long expectedSeconds) {
        assertEquals(Duration.ofSeconds(expectedSeconds), DurationParser.parse(input));
    }

    @Test
    void throwsOnUnknownUnit() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("10x"));
    }

    @Test
    void throwsOnBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("  "));
    }

    @Test
    void throwsOnNullInput() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse(null));
    }

    @Test
    void throwsOnNonNumericValue() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("abch"));
    }
}
