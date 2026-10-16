package com.azamzia.payments.core;

import com.azamzia.payments.domain.PaymentEvent;

import java.util.Optional;

/** Outbound port for the system of record. Implementations may fail transiently. */
public interface LedgerPort {

    void apply(PaymentEvent event);

    Optional<PaymentSummary> summary(String paymentId);
}
