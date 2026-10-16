package com.azamzia.payments.core;

import java.time.Duration;

/**
 * Exponential back-off with a ceiling. Mirrors the settings passed to the Spring Kafka error handler so
 * the behaviour can be unit tested without a broker.
 */
public record BackoffPolicy(int maxAttempts, Duration initialDelay, double multiplier, Duration maxDelay) {

    public BackoffPolicy {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        if (multiplier < 1.0) throw new IllegalArgumentException("multiplier must be >= 1.0");
    }

    /** Delay before the given retry (1-based). */
    public Duration delayBefore(int retry) {
        if (retry < 1) throw new IllegalArgumentException("retry must be >= 1");
        double millis = initialDelay.toMillis() * Math.pow(multiplier, retry - 1);
        return Duration.ofMillis((long) Math.min(millis, maxDelay.toMillis()));
    }

    public boolean shouldRetry(int attemptsSoFar) {
        return attemptsSoFar < maxAttempts;
    }
}
