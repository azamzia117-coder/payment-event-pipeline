package com.azamzia.payments.adapter.web;

import com.azamzia.payments.core.LedgerPort;
import com.azamzia.payments.core.PaymentSummary;
import com.azamzia.payments.core.PipelineStats;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PipelineController {

    private final LedgerPort ledger;
    private final PipelineStats stats;

    public PipelineController(LedgerPort ledger, PipelineStats stats) {
        this.ledger = ledger;
        this.stats = stats;
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentSummary> payment(@PathVariable String paymentId) {
        return ResponseEntity.of(ledger.summary(paymentId));
    }

    @GetMapping("/pipeline/stats")
    public PipelineStats.Snapshot stats() {
        return stats.snapshot();
    }
}
