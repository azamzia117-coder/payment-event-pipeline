package com.azamzia.payments.core;

/** Thrown for events that can never succeed; these go straight to the dead-letter topic. */
public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message) {
        super(message);
    }
}
