package com.azamzia.payments.domain;

import java.time.Instant;

/**
 * Immutable payment event as it arrives on the {@code payments.events} topic.
 * Amounts are stored in minor units (cents) to avoid floating-point rounding.
 */
public record PaymentEvent(
        String eventId,
        String paymentId,
        PaymentEventType type,
        long amountMinor,
        String currency,
        Instant occurredAt) {
}
