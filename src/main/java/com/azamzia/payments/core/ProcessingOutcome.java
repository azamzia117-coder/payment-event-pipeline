package com.azamzia.payments.core;

/** Result of handling one event. Exhaustive so callers must handle every case. */
public sealed interface ProcessingOutcome {

    record Applied(String eventId) implements ProcessingOutcome { }

    record Duplicate(String eventId) implements ProcessingOutcome { }

    record Rejected(String eventId, String reason) implements ProcessingOutcome { }
}
