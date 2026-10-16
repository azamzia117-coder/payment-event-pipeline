package com.azamzia.payments.core;

import java.util.concurrent.atomic.AtomicLong;

/** Counters exposed through the REST API and useful for alerting. */
public class PipelineStats {

    private final AtomicLong applied = new AtomicLong();
    private final AtomicLong duplicates = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong transientFailures = new AtomicLong();

    void recordApplied() { applied.incrementAndGet(); }
    void recordDuplicate() { duplicates.incrementAndGet(); }
    void recordRejected() { rejected.incrementAndGet(); }
    void recordTransientFailure() { transientFailures.incrementAndGet(); }

    public Snapshot snapshot() {
        return new Snapshot(applied.get(), duplicates.get(), rejected.get(), transientFailures.get());
    }

    public record Snapshot(long applied, long duplicates, long rejected, long transientFailures) { }
}
