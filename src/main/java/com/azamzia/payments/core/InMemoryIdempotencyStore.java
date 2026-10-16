package com.azamzia.payments.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe in-memory store suitable for local runs and tests. */
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    @Override
    public boolean markIfAbsent(String eventId) {
        return seen.add(eventId);
    }

    @Override
    public void release(String eventId) {
        seen.remove(eventId);
    }

    public int size() {
        return seen.size();
    }
}
