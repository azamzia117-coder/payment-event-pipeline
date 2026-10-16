package com.azamzia.payments.core;

/** Running totals for a single payment, in minor units. */
public record PaymentSummary(
        String paymentId,
        String currency,
        long authorizedMinor,
        long capturedMinor,
        long refundedMinor,
        boolean voided) {

    public long netSettledMinor() {
        return capturedMinor - refundedMinor;
    }
}
