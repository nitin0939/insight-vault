package com.ai.applications.rag.insightvault.service;

import com.ai.applications.rag.insightvault.config.StorageProperties;
import com.ai.applications.rag.insightvault.models.AppUser;
import com.ai.applications.rag.insightvault.models.DocumentUploadedEvent;
import com.ai.applications.rag.insightvault.models.StoredDocument;
import com.ai.applications.rag.insightvault.models.entity.KnowledgeDocument;
import com.ai.applications.rag.insightvault.repository.AppUserRepository;
import com.ai.applications.rag.insightvault.repository.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final ApplicationEventPublisher eventPublisher;
    private final AppUserRepository userRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final StorageProperties storageProperties;

    public DocumentService(ApplicationEventPublisher eventPublisher,
                          AppUserRepository userRepository,
                          KnowledgeDocumentRepository documentRepository,
                          DocumentStorageService storageService,
                          StorageProperties storageProperties) {
        this.eventPublisher = eventPublisher;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
    }

    @Transactional
    public UUID upload(MultipartFile file, String username) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A document is required.");
        }
        if (file.getSize() > storageProperties.maxFileSizeBytes()) {
            throw new IllegalArgumentException("File exceeds the configured upload limit.");
        }

        AppUser owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        StoredDocument storedDocument = storageService.store(file);

        KnowledgeDocument knowledgeDocument = KnowledgeDocument.uploaded(
                file.getOriginalFilename(),
                storedDocument.storedFilename(),
                storedDocument.path().toString(),
                file.getContentType(),
                file.getSize(),
                storedDocument.checksum(),
                owner
        );

        KnowledgeDocument saved = documentRepository.save(knowledgeDocument);
        log.info("Document uploaded: id={} filename={} owner={} sizeBytes={}",
                saved.getId(), file.getOriginalFilename(), username, file.getSize());
        eventPublisher.publishEvent(new DocumentUploadedEvent(saved.getId()));
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocument> findVisibleDocuments(Authentication authentication) {
        String username = authentication.getName();
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return documentRepository.findAllByOrderByUploadedAtDesc();
        }
        return documentRepository.findByOwnerUsernameOrderByUploadedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public KnowledgeDocument findAuthorizedDocument(UUID documentId, Authentication authentication) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = document.getOwner().getUsername().equals(authentication.getName());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You are not allowed to view this document.");
        }
        return document;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocument> search(String query, Authentication authentication) {
        List<KnowledgeDocument> all = findVisibleDocuments(authentication);
        if (query == null || query.isBlank()) {
            return all;
        }

        String lower = query.toLowerCase();
        List<KnowledgeDocument> matches = new ArrayList<>();
        for (KnowledgeDocument document : all) {
            String filename = document.getOriginalFilename() == null ? "" : document.getOriginalFilename().toLowerCase();
            if (filename.contains(lower)) {
                matches.add(document);
            }
        }
        return matches;
    }
}