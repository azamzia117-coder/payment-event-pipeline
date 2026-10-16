package com.azamzia.payments.core;

/** Thrown when a downstream dependency is temporarily unavailable; the event is retried. */
public class TransientFailureException extends RuntimeException {
    public TransientFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
