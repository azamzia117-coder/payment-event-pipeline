package com.azamzia.payments.domain;

/** Lifecycle events emitted by a (synthetic) payment processor. */
public enum PaymentEventType {
    AUTHORIZED,
    CAPTURED,
    REFUNDED,
    VOIDED
}
