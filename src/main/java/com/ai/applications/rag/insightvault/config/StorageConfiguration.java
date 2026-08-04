package com.ai.applications.rag.insightvault.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class StorageConfiguration {

    @Bean
    ApplicationRunner storageInitializer(StorageProperties properties) {
        return args -> {
            Path root = Path.of(properties.rootLocation());
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
        };
    }
}
