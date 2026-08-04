package com.ai.applications.rag.insightvault.service;

import com.ai.applications.rag.insightvault.config.StorageProperties;
import com.ai.applications.rag.insightvault.models.StoredDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    private final StorageProperties storageProperties;

    public DocumentStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public StoredDocument store(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("A document is required.");
            }
            if (file.getSize() > storageProperties.maxFileSizeBytes()) {
                throw new IllegalArgumentException("File exceeds the configured upload limit.");
            }

            String originalFilename = file.getOriginalFilename();
            String sanitized = originalFilename == null || originalFilename.isBlank()
                    ? "document" + UUID.randomUUID()
                    : originalFilename;

            String storedFilename = UUID.randomUUID() + "-" + sanitized.replaceAll("[\\\\/:*?\"<>|]", "_");
            Path rootLocation = Path.of(storageProperties.rootLocation());
            Files.createDirectories(rootLocation);

            Path targetPath = rootLocation.resolve(storedFilename);
            file.transferTo(targetPath);

            String checksum = sha256(file);
            log.debug("Stored file '{}' as '{}' ({} bytes, checksum={})",
                    originalFilename, storedFilename, file.getSize(), checksum);
            return new StoredDocument(storedFilename, targetPath, checksum);
        } catch (IOException e) {
            log.error("Failed to save uploaded document '{}'", file.getOriginalFilename(), e);
            throw new IllegalStateException("Could not save uploaded document.", e);
        }
    }

    public void delete(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                Files.delete(path);
                log.debug("Deleted stored file '{}'", path);
            }
        } catch (IOException e) {
            log.error("Failed to delete stored document '{}'", path, e);
            throw new IllegalStateException("Could not delete stored document.", e);
        }
    }

    private String sha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = file.getBytes();
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to calculate checksum.", e);
        }
    }
}
