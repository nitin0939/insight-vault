package com.ai.applications.rag.insightvault.repository;

import com.ai.applications.rag.insightvault.models.DocumentStatus;
import com.ai.applications.rag.insightvault.models.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    @EntityGraph(attributePaths = "owner")
    List<KnowledgeDocument> findByOwnerUsernameOrderByUploadedAtDesc(String username);

    @EntityGraph(attributePaths = "owner")
    List<KnowledgeDocument> findByOwnerIdOrderByUploadedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = "owner")
    List<KnowledgeDocument> findAllByOrderByUploadedAtDesc();

    long countByStatus(DocumentStatus status);
}
