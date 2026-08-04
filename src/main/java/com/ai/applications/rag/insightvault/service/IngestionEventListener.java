package com.ai.applications.rag.insightvault.service;

import com.ai.applications.rag.insightvault.models.DocumentUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IngestionEventListener {

    private static final Logger log = LoggerFactory.getLogger(IngestionEventListener.class);

    private final IngestionProcessor ingestionProcessor;

    public IngestionEventListener(IngestionProcessor ingestionProcessor) {
        this.ingestionProcessor = ingestionProcessor;
    }

    @Async("ingestionExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void process(DocumentUploadedEvent event) {
        log.debug("Received upload event for documentId={}, dispatching to ingestion executor", event.documentId());
        ingestionProcessor.process(event.documentId());
    }
}