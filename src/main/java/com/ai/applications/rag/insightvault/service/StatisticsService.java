package com.ai.applications.rag.insightvault.service;

import com.ai.applications.rag.insightvault.models.DocumentStatus;
import com.ai.applications.rag.insightvault.models.IngestionStatistics;
import com.ai.applications.rag.insightvault.models.entity.KnowledgeDocument;
import com.ai.applications.rag.insightvault.repository.KnowledgeDocumentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class StatisticsService {

    private final KnowledgeDocumentRepository repository;

    public StatisticsService(KnowledgeDocumentRepository repository) {
        this.repository = repository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public IngestionStatistics getStatistics() {
        List<KnowledgeDocument> allDocuments = repository.findAllByOrderByUploadedAtDesc();
        long totalDocuments = allDocuments.size();
        long uploadedDocuments = repository.countByStatus(DocumentStatus.UPLOADED);
        long processingDocuments = repository.countByStatus(DocumentStatus.PROCESSING);
        long readyDocuments = repository.countByStatus(DocumentStatus.READY);
        long failedDocuments = repository.countByStatus(DocumentStatus.FAILED);

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        long documentsUploadedToday = allDocuments.stream()
                .filter(document -> document.getUploadedAt() != null)
                .filter(document -> document.getUploadedAt().atZone(ZoneId.systemDefault()).toLocalDate().isEqual(today))
                .count();

        double averageChunksPerDocument = totalDocuments == 0 ? 0 : allDocuments.stream()
                .mapToInt(KnowledgeDocument::getChunkCount)
                .average()
                .orElse(0.0);

        long totalChunksIndexed = allDocuments.stream().mapToLong(KnowledgeDocument::getChunkCount).sum();
        long totalStorageBytes = allDocuments.stream().mapToLong(KnowledgeDocument::getFileSize).sum();
        long activeUsers = allDocuments.stream()
                .map(KnowledgeDocument::getOwner)
                .filter(Objects::nonNull)
                .map(owner -> owner.getUsername())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new IngestionStatistics(
                totalDocuments,
                uploadedDocuments,
                processingDocuments,
                readyDocuments,
                failedDocuments,
                documentsUploadedToday,
                averageChunksPerDocument,
                totalChunksIndexed,
                totalStorageBytes,
                activeUsers
        );
    }
}