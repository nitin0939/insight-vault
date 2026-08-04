package com.ai.applications.rag.insightvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InsightVaultApplication {

	public static void main(String[] args) {
		SpringApplication.run(InsightVaultApplication.class, args);
	}

}
