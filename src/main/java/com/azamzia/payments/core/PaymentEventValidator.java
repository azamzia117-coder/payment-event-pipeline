package com.azamzia.payments.core;

import com.azamzia.payments.domain.PaymentEvent;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/** Structural validation applied before any side effect happens. */
public class PaymentEventValidator {

    public List<String> validate(PaymentEvent event) {
        List<String> errors = new ArrayList<>();
        if (event == null) {
            errors.add("event is null");
            return errors;
        }
        if (isBlank(event.eventId())) errors.add("eventId is required");
        if (isBlank(event.paymentId())) errors.add("paymentId is required");
        if (event.type() == null) errors.add("type is required");
        if (event.amountMinor() <= 0) errors.add("amountMinor must be positive");
        if (event.occurredAt() == null) errors.add("occurredAt is required");
        if (isBlank(event.currency())) {
            errors.add("currency is required");
        } else {
            try {
                Currency.getInstance(event.currency());
            } catch (IllegalArgumentException e) {
                errors.add("currency is not ISO-4217: " + event.currency());
            }
        }
        return errors;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
