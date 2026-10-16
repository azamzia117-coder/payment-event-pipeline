package com.azamzia.payments.adapter.kafka;

import com.azamzia.payments.core.InvalidEventException;
import com.azamzia.payments.core.PaymentEventHandler;
import com.azamzia.payments.core.ProcessingOutcome;
import com.azamzia.payments.domain.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Thin adapter: translates Kafka records into calls on the framework-free handler. */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final PaymentEventHandler handler;

    public PaymentEventListener(PaymentEventHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(topics = "${pipeline.topic:payments.events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onEvent(PaymentEvent event) {
        ProcessingOutcome outcome = handler.handle(event);
        switch (outcome) {
            case ProcessingOutcome.Applied applied -> log.debug("Applied {}", applied.eventId());
            case ProcessingOutcome.Duplicate duplicate -> log.info("Skipped duplicate {}", duplicate.eventId());
            // Throwing hands the record to the error handler, which routes it to the DLT without retrying.
            case ProcessingOutcome.Rejected rejected -> throw new InvalidEventException(rejected.reason());
        }
    }
}
