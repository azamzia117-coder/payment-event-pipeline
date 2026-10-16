package com.azamzia.payments.core;

import com.azamzia.payments.domain.PaymentEvent;
import com.azamzia.payments.domain.PaymentEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEventHandlerTest {

    private InMemoryLedger ledger;
    private InMemoryIdempotencyStore store;
    private PipelineStats stats;
    private PaymentEventHandler handler;

    @BeforeEach
    void setUp() {
        ledger = new InMemoryLedger();
        store = new InMemoryIdempotencyStore();
        stats = new PipelineStats();
        handler = new PaymentEventHandler(new PaymentEventValidator(), store, ledger, stats);
    }

    private static PaymentEvent event(String id, String paymentId, PaymentEventType type, long amount) {
        return new PaymentEvent(id, paymentId, type, amount, "USD", Instant.parse("2026-01-15T10:00:00Z"));
    }

    @Test
    void appliesValidEventAndUpdatesLedger() {
        ProcessingOutcome outcome = handler.handle(event("e1", "p1", PaymentEventType.AUTHORIZED, 5_000));

        assertInstanceOf(ProcessingOutcome.Applied.class, outcome);
        assertEquals(5_000L, ledger.summary("p1").orElseThrow().authorizedMinor());
        assertEquals(1L, stats.snapshot().applied());
    }

    @Test
    void redeliveredEventIsIgnoredSoLedgerIsNotDoubleCounted() {
        PaymentEvent auth = event("e1", "p1", PaymentEventType.AUTHORIZED, 5_000);
        handler.handle(auth);
        ProcessingOutcome second = handler.handle(auth);

        assertInstanceOf(ProcessingOutcome.Duplicate.class, second);
        assertEquals(5_000L, ledger.summary("p1").orElseThrow().authorizedMinor());
        assertEquals(1L, stats.snapshot().duplicates());
    }

    @Test
    void structurallyInvalidEventIsRejectedWithoutTouchingLedger() {
        PaymentEvent bad = new PaymentEvent("e9", "p9", PaymentEventType.CAPTURED, -10, "XYZ1", null);

        ProcessingOutcome outcome = handler.handle(bad);

        ProcessingOutcome.Rejected rejected = assertInstanceOf(ProcessingOutcome.Rejected.class, outcome);
        assertTrue(rejected.reason().contains("amountMinor must be positive"));
        assertTrue(rejected.reason().contains("currency is not ISO-4217"));
        assertTrue(ledger.summary("p9").isEmpty());
    }

    @Test
    void captureAboveAuthorizationIsRejectedAsBusinessRuleViolation() {
        handler.handle(event("e1", "p1", PaymentEventType.AUTHORIZED, 1_000));

        ProcessingOutcome outcome = handler.handle(event("e2", "p1", PaymentEventType.CAPTURED, 1_500));

        assertInstanceOf(ProcessingOutcome.Rejected.class, outcome);
        assertEquals(0L, ledger.summary("p1").orElseThrow().capturedMinor());
    }

    @Test
    void transientLedgerFailureReleasesIdempotencyMarkSoRetrySucceeds() {
        AtomicInteger calls = new AtomicInteger();
        LedgerPort flaky = new LedgerPort() {
            @Override
            public void apply(PaymentEvent e) {
                if (calls.incrementAndGet() == 1) {
                    throw new IllegalStateException("database connection reset");
                }
                ledger.apply(e);
            }

            @Override
            public Optional<PaymentSummary> summary(String paymentId) {
                return ledger.summary(paymentId);
            }
        };
        PaymentEventHandler flakyHandler = new PaymentEventHandler(new PaymentEventValidator(), store, flaky, stats);
        PaymentEvent auth = event("e1", "p1", PaymentEventType.AUTHORIZED, 2_000);

        assertThrows(TransientFailureException.class, () -> flakyHandler.handle(auth));
        ProcessingOutcome retry = flakyHandler.handle(auth);

        assertInstanceOf(ProcessingOutcome.Applied.class, retry);
        assertEquals(2_000L, ledger.summary("p1").orElseThrow().authorizedMinor());
        assertEquals(1L, stats.snapshot().transientFailures());
    }

    @Test
    void fullLifecycleProducesCorrectNetSettlement() {
        handler.handle(event("e1", "p1", PaymentEventType.AUTHORIZED, 10_000));
        handler.handle(event("e2", "p1", PaymentEventType.CAPTURED, 8_000));
        handler.handle(event("e3", "p1", PaymentEventType.REFUNDED, 3_000));

        PaymentSummary s = ledger.summary("p1").orElseThrow();
        assertEquals(5_000L, s.netSettledMinor());
    }
}
