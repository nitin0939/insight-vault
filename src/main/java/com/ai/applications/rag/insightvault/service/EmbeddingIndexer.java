package com.ai.applications.rag.insightvault.service;

import com.ai.applications.rag.insightvault.models.entity.KnowledgeDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class EmbeddingIndexer {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIndexer.class);

    private final VectorStore vectorStore;

    public EmbeddingIndexer(@Qualifier("applicationVectorStore") VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void index(KnowledgeDocument source, List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            log.debug("EmbeddingIndexer: no chunks to index for documentId={}", source.getId());
            return;
        }

        List<Document> vectorDocuments = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            vectorDocuments.add(toVectorDocument(source, chunks.get(index), index));
        }

        log.debug("EmbeddingIndexer: writing {} chunk embeddings to vector store for documentId={}",
                vectorDocuments.size(), source.getId());
        vectorStore.add(vectorDocuments);
        log.debug("EmbeddingIndexer: vector store write complete for documentId={}", source.getId());
    }

    private Document toVectorDocument(KnowledgeDocument source, String chunkText, int chunkNumber) {
        Document document = new Document(chunkText);
        document.getMetadata().put("document_id", source.getId().toString());
        document.getMetadata().put("owner_user_id", source.getOwner().getId().toString());
        document.getMetadata().put("source_name", source.getOriginalFilename());
        document.getMetadata().put("chunk_number", chunkNumber);
        return document;
    }
}