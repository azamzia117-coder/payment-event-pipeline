package com.azamzia.payments.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackoffPolicyTest {

    private final BackoffPolicy policy =
            new BackoffPolicy(4, Duration.ofMillis(500), 2.0, Duration.ofSeconds(3));

    @Test
    void delaysGrowExponentiallyUntilCapped() {
        assertEquals(Duration.ofMillis(500), policy.delayBefore(1));
        assertEquals(Duration.ofMillis(1000), policy.delayBefore(2));
        assertEquals(Duration.ofMillis(2000), policy.delayBefore(3));
        assertEquals(Duration.ofMillis(3000), policy.delayBefore(4));
    }

    @Test
    void stopsRetryingAtMaxAttempts() {
        assertTrue(policy.shouldRetry(3));
        assertFalse(policy.shouldRetry(4));
    }

    @Test
    void rejectsNonsensicalConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> new BackoffPolicy(0, Duration.ofMillis(1), 2.0, Duration.ofMillis(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new BackoffPolicy(3, Duration.ofMillis(1), 0.5, Duration.ofMillis(1)));
    }
}
