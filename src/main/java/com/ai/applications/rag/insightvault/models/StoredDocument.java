package com.ai.applications.rag.insightvault.models;

import java.nio.file.Path;

public record StoredDocument(
        String storedFilename,
        Path path,
        String checksum
) {
}
