package com.azamzia.payments.core;

import com.azamzia.payments.domain.PaymentEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple ledger that enforces basic business invariants:
 * a payment cannot capture more than it authorized or refund more than it captured.
 */
public class InMemoryLedger implements LedgerPort {

    private final Map<String, PaymentSummary> payments = new ConcurrentHashMap<>();

    @Override
    public void apply(PaymentEvent e) {
        payments.compute(e.paymentId(), (id, current) -> {
            PaymentSummary s = current != null ? current
                    : new PaymentSummary(id, e.currency(), 0, 0, 0, false);
            if (!s.currency().equals(e.currency())) {
                throw new InvalidEventException("currency mismatch for payment " + id);
            }
            if (s.voided()) {
                throw new InvalidEventException("payment " + id + " is voided");
            }
            return switch (e.type()) {
                case AUTHORIZED -> new PaymentSummary(id, s.currency(),
                        s.authorizedMinor() + e.amountMinor(), s.capturedMinor(), s.refundedMinor(), false);
                case CAPTURED -> {
                    long captured = s.capturedMinor() + e.amountMinor();
                    if (captured > s.authorizedMinor()) {
                        throw new InvalidEventException("capture exceeds authorization for payment " + id);
                    }
                    yield new PaymentSummary(id, s.currency(), s.authorizedMinor(), captured, s.refundedMinor(), false);
                }
                case REFUNDED -> {
                    long refunded = s.refundedMinor() + e.amountMinor();
                    if (refunded > s.capturedMinor()) {
                        throw new InvalidEventException("refund exceeds captured amount for payment " + id);
                    }
                    yield new PaymentSummary(id, s.currency(), s.authorizedMinor(), s.capturedMinor(), refunded, false);
                }
                case VOIDED -> {
                    if (s.capturedMinor() > 0) {
                        throw new InvalidEventException("cannot void captured payment " + id);
                    }
                    yield new PaymentSummary(id, s.currency(), s.authorizedMinor(), 0, 0, true);
                }
            };
        });
    }

    @Override
    public Optional<PaymentSummary> summary(String paymentId) {
        return Optional.ofNullable(payments.get(paymentId));
    }
}
