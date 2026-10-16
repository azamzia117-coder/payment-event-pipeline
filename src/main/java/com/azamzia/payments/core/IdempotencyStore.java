package com.azamzia.payments.core;

/**
 * Records which event ids have already been applied so redelivered messages are harmless.
 * A production implementation would use a unique-keyed table or Redis SETNX with a TTL.
 */
public interface IdempotencyStore {

    /** @return true if the id was newly recorded, false if it had been seen before. */
    boolean markIfAbsent(String eventId);

    /** Removes the mark so a failed attempt can be retried. */
    void release(String eventId);
}
