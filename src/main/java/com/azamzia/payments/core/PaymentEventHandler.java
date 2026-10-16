package com.azamzia.payments.core;

import com.azamzia.payments.domain.PaymentEvent;

import java.util.List;

/**
 * Framework-free core of the consumer. The Kafka adapter calls {@link #handle(PaymentEvent)} for every
 * record; this class decides whether the event is applied, ignored as a duplicate, or rejected.
 *
 * <p>Transient failures are re-thrown as {@link TransientFailureException} so the messaging layer can
 * retry with back-off. The idempotency mark is released first so the retry is not mistaken for a duplicate.
 */
public class PaymentEventHandler {

    private static final System.Logger LOG = System.getLogger(PaymentEventHandler.class.getName());

    private final PaymentEventValidator validator;
    private final IdempotencyStore idempotencyStore;
    private final LedgerPort ledger;
    private final PipelineStats stats;

    public PaymentEventHandler(PaymentEventValidator validator, IdempotencyStore idempotencyStore,
                               LedgerPort ledger, PipelineStats stats) {
        this.validator = validator;
        this.idempotencyStore = idempotencyStore;
        this.ledger = ledger;
        this.stats = stats;
    }

    public ProcessingOutcome handle(PaymentEvent event) {
        List<String> errors = validator.validate(event);
        String eventId = event == null ? "<null>" : event.eventId();
        if (!errors.isEmpty()) {
            stats.recordRejected();
            LOG.log(System.Logger.Level.WARNING, "Rejected event {0}: {1}", eventId, errors);
            return new ProcessingOutcome.Rejected(eventId, String.join("; ", errors));
        }
        if (!idempotencyStore.markIfAbsent(event.eventId())) {
            stats.recordDuplicate();
            LOG.log(System.Logger.Level.DEBUG, "Duplicate event {0} ignored", eventId);
            return new ProcessingOutcome.Duplicate(eventId);
        }
        try {
            ledger.apply(event);
            stats.recordApplied();
            return new ProcessingOutcome.Applied(eventId);
        } catch (InvalidEventException e) {
            // Business rule violation: keep the idempotency mark so replays are also ignored.
            stats.recordRejected();
            LOG.log(System.Logger.Level.WARNING, "Business rule rejected {0}: {1}", eventId, e.getMessage());
            return new ProcessingOutcome.Rejected(eventId, e.getMessage());
        } catch (RuntimeException e) {
            idempotencyStore.release(event.eventId());
            stats.recordTransientFailure();
            throw new TransientFailureException("Ledger unavailable for event " + eventId, e);
        }
    }
}
