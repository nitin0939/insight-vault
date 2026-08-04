package com.ai.applications.rag.insightvault.service;

import com.ai.applications.rag.insightvault.models.DocumentUploadedEvent;
import com.ai.applications.rag.insightvault.models.entity.KnowledgeDocument;
import com.ai.applications.rag.insightvault.repository.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.UUID;

@Service
public class AdminDocumentService {

    private static final Logger log = LoggerFactory.getLogger(AdminDocumentService.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final VectorStore vectorStore;
    private final ApplicationEventPublisher eventPublisher;

    public AdminDocumentService(KnowledgeDocumentRepository documentRepository,
                               DocumentStorageService storageService,
                               @Qualifier("applicationVectorStore") VectorStore vectorStore,
                               ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.vectorStore = vectorStore;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteDocument(UUID documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        log.info("Deleting document: id={} filename={}", documentId, document.getOriginalFilename());

        if (document.getId() != null) {
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            vectorStore.delete(builder.eq("document_id", document.getId().toString()).build());
        }

        document.markDeleting();
        documentRepository.save(document);

        if (document.getStoragePath() != null && !document.getStoragePath().isBlank()) {
            storageService.delete(Path.of(document.getStoragePath()));
        }

        documentRepository.delete(document);
        log.info("Document deleted: id={}", documentId);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void reprocessDocument(UUID documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        log.info("Reprocessing document: id={} filename={}", documentId, document.getOriginalFilename());

        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.eq("document_id", document.getId().toString()).build());

        eventPublisher.publishEvent(new DocumentUploadedEvent(document.getId()));
    }
}