package com.azamzia.payments.core;

import com.azamzia.payments.domain.PaymentEvent;
import com.azamzia.payments.domain.PaymentEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLedgerTest {

    private final InMemoryLedger ledger = new InMemoryLedger();

    private static PaymentEvent e(PaymentEventType t, long amt, String ccy) {
        return new PaymentEvent("id-" + t + amt, "p1", t, amt, ccy, Instant.EPOCH);
    }

    @Test
    void refundCannotExceedCapturedAmount() {
        ledger.apply(e(PaymentEventType.AUTHORIZED, 1_000, "USD"));
        ledger.apply(e(PaymentEventType.CAPTURED, 500, "USD"));
        assertThrows(InvalidEventException.class, () -> ledger.apply(e(PaymentEventType.REFUNDED, 600, "USD")));
    }

    @Test
    void currencyCannotChangeMidPayment() {
        ledger.apply(e(PaymentEventType.AUTHORIZED, 1_000, "USD"));
        assertThrows(InvalidEventException.class, () -> ledger.apply(e(PaymentEventType.CAPTURED, 100, "EUR")));
    }

    @Test
    void voidedPaymentAcceptsNoFurtherEvents() {
        ledger.apply(e(PaymentEventType.AUTHORIZED, 1_000, "USD"));
        ledger.apply(e(PaymentEventType.VOIDED, 1_000, "USD"));
        assertTrue(ledger.summary("p1").orElseThrow().voided());
        assertThrows(InvalidEventException.class, () -> ledger.apply(e(PaymentEventType.CAPTURED, 100, "USD")));
    }
}
