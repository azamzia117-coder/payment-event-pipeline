package com.azamzia.payments.config;

import com.azamzia.payments.core.BackoffPolicy;
import com.azamzia.payments.core.IdempotencyStore;
import com.azamzia.payments.core.InMemoryIdempotencyStore;
import com.azamzia.payments.core.InMemoryLedger;
import com.azamzia.payments.core.InvalidEventException;
import com.azamzia.payments.core.LedgerPort;
import com.azamzia.payments.core.PaymentEventHandler;
import com.azamzia.payments.core.PaymentEventValidator;
import com.azamzia.payments.core.PipelineStats;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;

import java.time.Duration;

@Configuration
public class PipelineConfig {

    @Bean
    PipelineStats pipelineStats() {
        return new PipelineStats();
    }

    @Bean
    IdempotencyStore idempotencyStore() {
        return new InMemoryIdempotencyStore();
    }

    @Bean
    LedgerPort ledgerPort() {
        return new InMemoryLedger();
    }

    @Bean
    PaymentEventHandler paymentEventHandler(IdempotencyStore store, LedgerPort ledger, PipelineStats stats) {
        return new PaymentEventHandler(new PaymentEventValidator(), store, ledger, stats);
    }

    @Bean
    BackoffPolicy backoffPolicy(@Value("${pipeline.retry.max-attempts:4}") int maxAttempts,
                                @Value("${pipeline.retry.initial-delay-ms:500}") long initialDelayMs,
                                @Value("${pipeline.retry.multiplier:2.0}") double multiplier,
                                @Value("${pipeline.retry.max-delay-ms:8000}") long maxDelayMs) {
        return new BackoffPolicy(maxAttempts, Duration.ofMillis(initialDelayMs), multiplier, Duration.ofMillis(maxDelayMs));
    }

    /**
     * Retries transient failures with exponential back-off, then publishes the record to
     * {@code <topic>.DLT}. Poison messages (bad JSON, invalid events) skip retries entirely.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template, BackoffPolicy policy) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));
        ExponentialBackOff backOff = new ExponentialBackOff(policy.initialDelay().toMillis(), policy.multiplier());
        backOff.setMaxInterval(policy.maxDelay().toMillis());
        backOff.setMaxAttempts(policy.maxAttempts() - 1);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(InvalidEventException.class, DeserializationException.class);
        return handler;
    }
}
