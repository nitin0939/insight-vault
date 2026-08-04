package com.ai.applications.rag.insightvault.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorStoreConfiguration {

    /**
     * Create the PGVector store explicitly so the backing table is initialized before
     * the ingestion pipeline starts inserting document chunks. The default Spring AI
     * auto-configuration is kept as-is, but this custom bean is the one used by the
     * application so the first index write reliably creates the table in PostgreSQL.
     */
    @Bean(name = "applicationVectorStore")
    @Primary
    VectorStore applicationVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(embeddingModel.dimensions())
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .build();
    }
}
