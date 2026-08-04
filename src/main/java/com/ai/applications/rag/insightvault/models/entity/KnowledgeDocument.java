package com.ai.applications.rag.insightvault.models.entity;

import com.ai.applications.rag.insightvault.models.AppUser;
import com.ai.applications.rag.insightvault.models.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename;

    @Column(nullable = false)
    private String storagePath;

    @Column(nullable = false)
    private String checksum;

    @Column(nullable = false)
    private long fileSize;

    @Column
    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id")
    private AppUser owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(nullable = false)
    private int chunkCount = 0;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column
    private Instant processedAt;

    @Column(length = 2000)
    private String failureReason;

    protected KnowledgeDocument() {
    }

    public static KnowledgeDocument uploaded(
            String originalFilename,
            String storedFilename,
            String storagePath,
            String contentType,
            long fileSize,
            String checksum,
            AppUser owner
    ) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.originalFilename = originalFilename;
        document.storedFilename = storedFilename;
        document.storagePath = storagePath;
        document.contentType = contentType;
        document.fileSize = fileSize;
        document.checksum = checksum;
        document.owner = owner;
        document.status = DocumentStatus.UPLOADED;
        document.chunkCount = 0;
        document.uploadedAt = Instant.now();
        return document;
    }

    @PrePersist
    void beforeSave() {
        if (this.uploadedAt == null) {
            this.uploadedAt = Instant.now();
        }
    }

    public void markProcessing() {
        this.status = DocumentStatus.PROCESSING;
        this.failureReason = null;
    }

    public void markReady(int chunkCount) {
        this.status = DocumentStatus.READY;
        this.chunkCount = chunkCount;
        this.processedAt = Instant.now();
    }

    public void markFailed(String failureReason) {
        this.status = DocumentStatus.FAILED;
        this.failureReason = failureReason;
    }

    public void markDeleting() {
        this.status = DocumentStatus.DELETED;
    }

    public UUID getId() {
        return id;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public AppUser getOwner() {
        return owner;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getContentType() {
        return contentType;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}